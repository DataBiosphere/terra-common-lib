package bio.terra.common.stairway;

import bio.terra.stairway.DynamicHook;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.HookAction;
import bio.terra.stairway.StairwayHook;
import bio.terra.stairway.Step;
import com.google.common.base.Stopwatch;
import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Link;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.SpanContextParseException;
import java.util.Base64;
import java.util.Optional;
import org.apache.commons.lang3.ClassUtils;

/**
 * A {@link StairwayHook} to add support for tracing execution of Stairway flights with OpenCensus.
 *
 * <p>This hook uses Spans at 3 different levels. From lowest to highest:
 *
 * <ul>
 *   <li>A Span around the execution of each Step, one Span per Step execution. All of the {@link
 *       Step#doStep(FlightContext)} {@link Step#undoStep(FlightContext)} will be executed in the
 *       context of this Span.
 *   <li>A Span around the execution of each Flight, one Span per {@link Flight}. Note that a single
 *       Flight may be run multiple times if its execution is paused and resumed. Each Step Span is
 *       a child Span of a Flight Span.
 *   <li>A linked Span for the entire Flight submission. Each Flight Span has a parent link to the
 *       submission span. See <a href="https://opencensus.io/tracing/span/link/">OpenCensus
 *       Link</a>. The submission span is used to correlate all of the Flight Spans together. The
 *       submission Span may be created outside of the context of this hook and passed in with the
 *       input FlightMap; this allows all of the Flight Spans to be linked to the request Span that
 *       initiated the Flight. See {@link #serializeCurrentTracingContext()} {@link
 *       #SUBMISSION_SPAN_CONTEXT_MAP_KEY}. If no submission Span is passed into the Flight, a dummy
 *       Submission span is created by this hook so that all Flight Spans are still correlated.
 * </ul>
 *
 * <p>If a Span is not ended, it will not be exported to the tracing service.
 *
 * @see <a href="https://opencensus.io/tracing/">https://opencensus.io/tracing/</a>
 */
public class TracingHook implements StairwayHook {
  /** The {@link FlightMap} key for the submission Span's context. */
  public static final String SUBMISSION_SPAN_CONTEXT_MAP_KEY = "opencensusTracingSpanContext";

  // Prefixes to use for Span names. Standard prefixes make it easier to search for all Spans of
  // different types.
  private static final String SUBMISSION_NAME_PREFIX = "stairway/submission/";
  private static final String FLIGHT_NAME_PREFIX = "stairway/flight/";
  private static final String STEP_NAME_PREFIX = "stairway/step/";

  private final Tracer tracer = Tracing.getTracer();

  /**
   * Serialize the current Span's {@link SpanContext}. To be used to store a submission Span for the
   * Flight with {@link #SUBMISSION_SPAN_CONTEXT_MAP_KEY}.
   *
   * @return Serialized context
   */
  public static Object serializeCurrentTracingContext() {
    return encodeContext(Tracing.getTracer().getCurrentSpan().getContext());
  }

  /**
   * Store the current Span's {@link SpanContext} as the submission Span.
   *
   * @param inputMap flight map to use to store the context
   */
  public static void storeCurrentContextAsSubmission(FlightMap inputMap) {
    inputMap.put(SUBMISSION_SPAN_CONTEXT_MAP_KEY, serializeCurrentTracingContext());
  }

  @Override
  public Optional<DynamicHook> flightFactory(FlightContext context) {
    return Optional.of(new TraceFlightHook());
  }

  @Override
  public Optional<DynamicHook> stepFactory(FlightContext context) {
    return Optional.of(new TraceStepHook());
  }

  /**
   * A {@link DynamicHook} for creating Spans for each Flight execution.
   *
   * <p>Note that the same Flight may be run multiple times by Stairway.
   */
  private class TraceFlightHook implements DynamicHook {
    private Scope flightScope;
    private Stopwatch stopwatch;

    @Override
    public HookAction start(FlightContext flightContext) {
      SpanContext submissionContext = getOrCreateSubmissionContext(flightContext);
      // Start the Flight Span and its Scope. We rely on implicit propagation to get this Flight
      // Span as the current span during Step execution. We must remember to close the Flight Scope
      // at the end of the Flight's current run.
      flightScope =
          tracer
              .spanBuilderWithRemoteParent(
                  FLIGHT_NAME_PREFIX
                      + ClassUtils.getShortClassName(flightContext.getFlightClassName()),
                  submissionContext)
              .startScopedSpan();
      stopwatch = Stopwatch.createStarted();
      Span flightSpan = tracer.getCurrentSpan();
      flightSpan.addLink(Link.fromSpanContext(submissionContext, Link.Type.PARENT_LINKED_SPAN));
      flightSpan.putAttribute(
          "stairway/flightId", AttributeValue.stringAttributeValue(flightContext.getFlightId()));
      flightSpan.putAttribute(
          "stairway/flightClass",
          AttributeValue.stringAttributeValue(flightContext.getFlightClassName()));
      // Start the Scope of the Flight Span's execution. We rely on implicit Span propagation to get
      // the scope for the step's execution. We must remember to close the Scope at the end of the
      // Flight.
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction end(FlightContext flightContext) {
      Span flightSpan = tracer.getCurrentSpan();
      flightSpan.putAttribute(
          "flightStatus",
          AttributeValue.stringAttributeValue(flightContext.getFlightStatus().toString()));
      flightScope.close();
      if (stopwatch != null) {
        MetricsHelper.recordLatency(flightContext.getFlightClassName(), stopwatch.elapsed());
        stopwatch = null;
      }
      MetricsHelper.recordError(
          flightContext.getFlightClassName(), flightContext.getFlightStatus());
      return HookAction.CONTINUE;
    }
  }

  /** A {@link DynamicHook} for creating Spans for each Step execution. */
  private class TraceStepHook implements DynamicHook {
    private Scope stepScope;

    @Override
    public HookAction start(FlightContext flightContext) {
      // We rely on the implicit propagation of spans. By entering the scope of the Flight Span at
      // the start of the flight, the current span here should be that same Flight Span.
      // Start the Scope of the Step Span's execution so that the step execution has a relevant
      // current span. We must remember to close the Scope at the end of the Step.
      stepScope =
          tracer
              .spanBuilder(
                  STEP_NAME_PREFIX + ClassUtils.getShortClassName(flightContext.getStepClassName()))
              .startScopedSpan();
      Span stepSpan = tracer.getCurrentSpan();
      stepSpan.putAttribute(
          "stairway/flightId", AttributeValue.stringAttributeValue(flightContext.getFlightId()));
      stepSpan.putAttribute(
          "stairway/flightClass",
          AttributeValue.stringAttributeValue(flightContext.getFlightClassName()));
      stepSpan.putAttribute(
          "stairway/stepClass",
          AttributeValue.stringAttributeValue(flightContext.getStepClassName()));
      stepSpan.putAttribute(
          "stairway/stepIndex", AttributeValue.longAttributeValue(flightContext.getStepIndex()));
      stepSpan.putAttribute(
          "stairway/direction",
          AttributeValue.stringAttributeValue(flightContext.getDirection().toString()));
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction end(FlightContext flightContext) {
      stepScope.close();
      return HookAction.CONTINUE;
    }
  }

  /**
   * Gets or creates the context of the submission Span. Each Flight span is linked to the
   * submission span to correlate all Flight Spans.
   */
  private SpanContext getOrCreateSubmissionContext(FlightContext flightContext) {
    // Check the input parameters for a submission span to link for the Flight.
    String encodedContext =
        flightContext.getInputParameters().get(SUBMISSION_SPAN_CONTEXT_MAP_KEY, String.class);
    if (encodedContext != null) {
      return decodeContext(encodedContext);
    }
    // Check the working map for a submission span to link for the Flight.
    encodedContext =
        flightContext.getWorkingMap().get(SUBMISSION_SPAN_CONTEXT_MAP_KEY, String.class);
    if (encodedContext != null) {
      return decodeContext(encodedContext);
    }
    // Create a new span to use as a submission span to link for the Flight Spans.
    Span submissionSpan =
        tracer
            .spanBuilderWithExplicitParent(
                SUBMISSION_NAME_PREFIX
                    + ClassUtils.getShortClassName(flightContext.getFlightClassName()),
                null)
            .startSpan();
    submissionSpan.putAttribute(
        "stairway/flightId", AttributeValue.stringAttributeValue(flightContext.getFlightId()));
    submissionSpan.putAttribute(
        "stairway/flightClass",
        AttributeValue.stringAttributeValue(flightContext.getFlightClassName()));

    SpanContext flightSpanContext = submissionSpan.getContext();
    // Store the submission span so that there is only one of these per Flight ID.
    flightContext
        .getWorkingMap()
        .put(SUBMISSION_SPAN_CONTEXT_MAP_KEY, encodeContext(flightSpanContext));

    // End the span immediately. We cannot carry this span until the flight finishes, we merely use
    // it to link the Flight Spans.
    submissionSpan.end();
    return flightSpanContext;
  }

  /**
   * Encode the SpanContext as a base64 string using the binary format. Works with {@link
   * #decodeContext(String)}.
   */
  private static String encodeContext(SpanContext spanContext) {
    return Base64.getEncoder()
        .encodeToString(
            Tracing.getPropagationComponent().getBinaryFormat().toByteArray(spanContext));
  }

  /**
   * Decode the SpanContext from a base64 string using the binary format. Works with {@link
   * #encodeContext(SpanContext)}.
   */
  private static SpanContext decodeContext(String encodedContext) {
    try {
      return Tracing.getPropagationComponent()
          .getBinaryFormat()
          .fromByteArray(Base64.getDecoder().decode(encodedContext));
    } catch (SpanContextParseException e) {
      throw new RuntimeException("Unable to decode SpanContext", e);
    }
  }
}
