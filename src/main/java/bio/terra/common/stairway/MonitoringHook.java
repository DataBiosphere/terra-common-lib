package bio.terra.common.stairway;

import bio.terra.stairway.DynamicHook;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.HookAction;
import bio.terra.stairway.StairwayHook;
import bio.terra.stairway.Step;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Stopwatch;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link StairwayHook} to add support for tracing execution of Stairway flights and record custom
 * metrics with OpenTelemetry.
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
 *       submission span. See <a
 *       href="https://opentelemetry.io/docs/concepts/signals/traces/#span-links/">OpenTelemetry
 *       Span Links</a>. The submission span is used to correlate all of the Flight Spans together.
 *       The submission Span may be created outside of the context of this hook and passed in with
 *       the input FlightMap; this allows all of the Flight Spans to be linked to the request Span
 *       that initiated the Flight. See {@link #serializeCurrentTracingContext(OpenTelemetry)}
 *       {@link #SUBMISSION_SPAN_CONTEXT_MAP_KEY}. If no submission Span is passed into the Flight,
 *       a dummy Submission span is created by this hook so that all Flight Spans are still
 *       correlated.
 * </ul>
 *
 * <p>If a Span is not ended, it will not be exported to the tracing service.
 *
 * @see <a href="https://opentelemetry.io/docs/concepts/signals/traces/">OpenTelemetry Traces</a>
 */
public class MonitoringHook implements StairwayHook {
  /** The {@link FlightMap} key for the submission Span's context. */
  public static final String SUBMISSION_SPAN_CONTEXT_MAP_KEY = "openTelemetryTracingSpanContext";

  // Prefixes to use for Span names. Standard prefixes make it easier to search for all Spans of
  // different types.
  private static final String SUBMISSION_NAME_PREFIX = "stairway/submission/";
  private static final String FLIGHT_NAME_PREFIX = "stairway/flight/";
  private static final String STEP_NAME_PREFIX = "stairway/step/";

  private static final TextMapSetter<Map<String, String>> CONTEXT_SETTER =
      (Map<String, String> carrier, String key, String value) -> carrier.put(key, value);

  private static final TextMapGetter<Map<String, String>> CONTEXT_GETTER =
      new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(Map<String, String> carrier) {
          return carrier.keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Map<String, String> carrier, String key) {
          return carrier.get(key);
        }
      };

  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;
  private final MetricsHelper metricsHelper;

  public MonitoringHook(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    this.tracer = openTelemetry.getTracer(getClass().getName());
    this.metricsHelper = new MetricsHelper(openTelemetry);
  }

  /**
   * Serialize the current Span's {@link SpanContext}. To be used to store a submission Span for the
   * Flight with {@link #SUBMISSION_SPAN_CONTEXT_MAP_KEY}.
   *
   * @return Serialized context
   */
  public static Map<String, String> serializeCurrentTracingContext(OpenTelemetry openTelemetry) {
    return encodeContext(Context.current(), openTelemetry);
  }

  /**
   * Store the current Span's {@link SpanContext} as the submission Span.
   *
   * @param inputMap flight map to use to store the context
   */
  public static void storeCurrentContextAsSubmission(
      FlightMap inputMap, OpenTelemetry openTelemetry) {
    inputMap.put(SUBMISSION_SPAN_CONTEXT_MAP_KEY, serializeCurrentTracingContext(openTelemetry));
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
      var submissionContext = getOrCreateSubmissionContext(flightContext);
      // Start the Flight Span and its Scope. We rely on implicit propagation to get this Flight
      // Span as the current span during Step execution. We must remember to close the Flight Scope
      // at the end of the Flight's current run.
      var flightSpan =
          tracer
              .spanBuilder(
                  FLIGHT_NAME_PREFIX
                      + ClassUtils.getShortClassName(flightContext.getFlightClassName()))
              .setParent(submissionContext)
              .addLink(Span.fromContext(submissionContext).getSpanContext())
              .setAttribute("stairway/flightId", flightContext.getFlightId())
              .setAttribute("stairway/flightClass", flightContext.getFlightClassName())
              .startSpan();
      // Start the Scope of the Flight Span's execution. We rely on implicit Span propagation to get
      // the scope for the step's execution. We must remember to close the Scope at the end of the
      // Flight.
      flightScope = flightSpan.makeCurrent();

      stopwatch = Stopwatch.createStarted();
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction end(FlightContext flightContext) {
      Span flightSpan = Span.current();
      flightSpan.setAttribute("flightStatus", flightContext.getFlightStatus().toString());
      flightSpan.end();
      flightScope.close();
      if (stopwatch != null) {
        metricsHelper.recordFlightLatency(
            flightContext.getFlightClassName(),
            flightContext.getFlightStatus(),
            stopwatch.elapsed());
        stopwatch = null;
      }
      metricsHelper.recordFlightError(
          flightContext.getFlightClassName(), flightContext.getFlightStatus());
      return HookAction.CONTINUE;
    }
  }

  /** A {@link DynamicHook} for creating Spans for each Step execution. */
  private class TraceStepHook implements DynamicHook {
    private Scope stepScope;
    private Stopwatch stopwatch;

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
              .setAttribute("stairway/flightId", flightContext.getFlightId())
              .setAttribute("stairway/flightClass", flightContext.getFlightClassName())
              .setAttribute("stairway/stepClass", flightContext.getStepClassName())
              .setAttribute("stairway/stepIndex", flightContext.getStepIndex())
              .setAttribute("stairway/direction", flightContext.getDirection().toString())
              .startSpan()
              .makeCurrent();
      stopwatch = Stopwatch.createStarted();
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction end(FlightContext flightContext) {
      Span.current().end();
      stepScope.close();
      if (stopwatch != null) {
        metricsHelper.recordStepLatency(
            flightContext.getFlightClassName(),
            flightContext.getDirection(),
            flightContext.getStepClassName(),
            stopwatch.elapsed());
        stopwatch = null;
      }
      metricsHelper.recordStepDirection(
          flightContext.getFlightClassName(),
          flightContext.getDirection(),
          flightContext.getStepClassName());
      return HookAction.CONTINUE;
    }
  }

  /**
   * Gets or creates the context of the submission Span. Each Flight span is linked to the
   * submission span to correlate all Flight Spans.
   */
  private Context getOrCreateSubmissionContext(FlightContext flightContext) {
    // Check the input parameters for a submission span to link for the Flight.
    Map<String, String> encodedContext =
        flightContext
            .getInputParameters()
            .get(SUBMISSION_SPAN_CONTEXT_MAP_KEY, new TypeReference<>() {});
    if (encodedContext != null) {
      return decodeContext(encodedContext, openTelemetry);
    }
    // Check the working map for a submission span to link for the Flight.
    encodedContext =
        flightContext
            .getWorkingMap()
            .get(SUBMISSION_SPAN_CONTEXT_MAP_KEY, new TypeReference<>() {});
    if (encodedContext != null) {
      return decodeContext(encodedContext, openTelemetry);
    }
    // Create a new span to use as a submission span to link for the Flight Spans.
    var spanBuilder = tracer.spanBuilder(SUBMISSION_NAME_PREFIX + flightContext.getFlightId());
    spanBuilder.setNoParent();
    spanBuilder.setAttribute("stairway/flightId", flightContext.getFlightId());
    spanBuilder.setAttribute("stairway/flightClass", flightContext.getFlightClassName());

    var flightSpan = spanBuilder.startSpan();
    try (var ignored = flightSpan.makeCurrent()) {
      // Store the submission span so that there is only one of these per Flight ID.
      flightContext
          .getWorkingMap()
          .put(SUBMISSION_SPAN_CONTEXT_MAP_KEY, encodeContext(Context.current(), openTelemetry));
      return Context.current();
    } finally {
      // End the span immediately. We cannot carry this span until the flight finishes, we merely
      // use
      // it to link the Flight Spans.
      flightSpan.end();
    }
  }

  /** Encode the Context as a Map. Works with {@link #decodeContext(Map, OpenTelemetry)}. */
  private static Map<String, String> encodeContext(Context context, OpenTelemetry openTelemetry) {
    var propagator = openTelemetry.getPropagators().getTextMapPropagator();
    var contextMap = new HashMap<String, String>();
    propagator.inject(context, contextMap, CONTEXT_SETTER);
    return contextMap;
  }

  /** Decode the Context from a Map. Works with {@link #encodeContext(Context, OpenTelemetry)}. */
  private static Context decodeContext(
      Map<String, String> encodedContext, OpenTelemetry openTelemetry) {
    var propagator = openTelemetry.getPropagators().getTextMapPropagator();
    return propagator.extract(Context.root(), encodedContext, CONTEXT_GETTER);
  }
}
