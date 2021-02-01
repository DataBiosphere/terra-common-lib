package bio.terra.common.tracing;

import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.HookAction;
import bio.terra.stairway.StairwayHook;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepHook;
import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Link;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.SpanContextParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link StairwayHook} to add support for tracing execution of Stairway flights with OpenCensus.
 *
 * <p>This hook uses Spans at 3 different levels. From lowest to highest:
 * <li>A Span around the execution of each Step, one Span per Step execution. All of the {@link
 *     Step#doStep(FlightContext)} {@link Step#undoStep(FlightContext)} will be executed in the
 *     context of this Span.
 * <li>A Span around the execution of each Flight, one Span per {@link Flight#run()}. Note that a
 *     single Flight may be run multiple times if its execution is paused and resumed. Each Step
 *     Span is a child Span of a Flight Span.
 * <li>A linked Span for the entire Flight submission. Each Flight Span has a parent link to the
 *     submission span. See <a href="https://opencensus.io/tracing/span/link/">OpenCensus Link</a>.
 *     The submission span is used to correlate all of the Flight Spans together. The submission
 *     Span may be created outside of the context of this hook and passed in with the input
 *     FlightMap; this allows all of the Flight Spans to be linked to the request Span that
 *     initiated the Flight. See {@link #serializeCurrentTracingContext()} {@link
 *     #SUBMISSION_SPAN_CONTEXT_MAP_KEY}. If no submission Span is passed into the Flight, a dummy
 *     Submission span is created by this hook so that all Flight Spans are still correlated.
 *
 *     <p>If a Span is not ended, it will not be exported to the tracing service.
 *
 * @see <a href="https://opencensus.io/tracing/">https://opencensus.io/tracing/</a>
 */
public class TracingHook implements StairwayHook {
  private final Tracer tracer = Tracing.getTracer();
  private final Logger logger = LoggerFactory.getLogger(TracingHook.class);

  // Prefixes to use for Span names. Standard prefixes make it easier to search for all Spans of
  // different types.
  private static final String SUBMISSION_NAME_PREFIX = "stairway/submission/";
  private static final String FLIGHT_NAME_PREFIX = "stairway/flight/";
  private static final String STEP_NAME_PREFIX = "stairway/step/";

  /** Map of Flight IDs to Flight Spans. */
  // DO NOT SUBMIT: Consider whether we want to have a FlightHook to match StepHook instead of
  // storing per Flight data in a global map. Note that the FlightHook would have to be able to make
  // StepHooks so that the StepHook can refer to data in the FlightHook.
  private final Map<String, Span> flightSpans = new ConcurrentHashMap<>();

  /** The {@link FlightMap} key for the submission Span's context. */
  public static final String SUBMISSION_SPAN_CONTEXT_MAP_KEY = "opencensusTracingSpanContext";

  /**
   * Serialize the current Span's {@link SpanContext}. To be used to store a submission Span for the
   * Flight with {@link #SUBMISSION_SPAN_CONTEXT_MAP_KEY}.
   */
  public static Object serializeCurrentTracingContext() {
    return encodeContext(Tracing.getTracer().getCurrentSpan().getContext());
  }

  /** Store the current Span's {@link SpanContext} as the submission Span. */
  public static void storeCurrentContextAsSubmission(FlightMap inputMap) {
    inputMap.put(SUBMISSION_SPAN_CONTEXT_MAP_KEY, serializeCurrentTracingContext());
  }

  /**
   * Creates a Flight Span for the current run of the Flight. Note that the same Flight may be run
   * multiple times by Stairway.
   */
  @Override
  public HookAction startFlight(FlightContext context) {
    SpanContext submissionContext = getOrSubmissionContext(context);
    // Start the Flight Span. We must remember to close the Flight Span at the end of the Flight's
    // current run.
    // Rather than use OpenCensus' scope-based span linkage, we manually attach step spans to this
    // parent span via the stepFactory method."
    Span flightSpan =
        Tracing.getTracer()
            .spanBuilder(
                FLIGHT_NAME_PREFIX + ClassUtils.getShortClassName(context.getFlightClassName()))
            .startSpan();
    flightSpan.addLink(Link.fromSpanContext(submissionContext, Link.Type.PARENT_LINKED_SPAN));
    flightSpan.putAttribute(
        "stairway/flightId", AttributeValue.stringAttributeValue(context.getFlightId()));
    flightSpan.putAttribute(
        "stairway/flightClass", AttributeValue.stringAttributeValue(context.getFlightClassName()));
    flightSpans.put(context.getFlightId(), flightSpan);
    return HookAction.CONTINUE;
  }

  /** Ends the Flight Span for the current run of the Flight. */
  @Override
  public HookAction endFlight(FlightContext context) {
    Span flightSpan = flightSpans.remove(context.getFlightId());
    flightSpan.putAttribute(
        "flightStatus", AttributeValue.stringAttributeValue(context.getFlightStatus().toString()));
    // End the Flight Span for the current run.
    flightSpan.end();
    return HookAction.CONTINUE;
  }

  @Override
  public Optional<StepHook> stepFactory(FlightContext context) {
    return Optional.of(new TraceStepHook(flightSpans.get(context.getFlightId())));
  }

  /** A {@link StepHook} for creating Spans for each Step execution. */
  private class TraceStepHook implements StepHook {
    private final Span flightSpan;
    private Span stepSpan = null;
    private Scope stepScope = null;

    public TraceStepHook(Span flightSpan) {
      this.flightSpan = flightSpan;
    }

    @Override
    public HookAction startStep(FlightContext flightContext) {
      // Start the Step Span. We must remember to close the Span at the end of the Step.
      stepSpan =
          Tracing.getTracer()
              .spanBuilderWithExplicitParent(
                  STEP_NAME_PREFIX + ClassUtils.getShortClassName(flightContext.getStepClassName()),
                  flightSpan)
              .startSpan();
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
      // Start the Scope of the Step Span's execution so that the step execution has a relevant
      // current span. We must remember to close the Scope at the end of the Step.
      stepScope = tracer.withSpan(stepSpan);
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction endStep(FlightContext flightContext) {
      stepScope.close();
      stepSpan.end();
      return HookAction.CONTINUE;
    }
  }

  /**
   * Gets or creates the context of the submission Span. Each Flight span is linked to the
   * submission span to correlate all Flight Spans.
   */
  private SpanContext getOrSubmissionContext(FlightContext flightContext) {
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
