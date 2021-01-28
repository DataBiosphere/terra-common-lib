package bio.terra.common.tracing;

import bio.terra.stairway.*;
import io.opencensus.common.Scope;
import io.opencensus.trace.*;
import io.opencensus.trace.propagation.SpanContextParseException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenCensusTracingHook implements StairwayHook {
  public static final String PARENT_SPAN_CONTEXT_MAP_KEY = "opencensusTracingSpanContext";
  private final Tracer tracer = Tracing.getTracer();
  private final Logger logger = LoggerFactory.getLogger(OpenCensusTracingHook.class);

  private final Map<String, Span> flightSpans = new ConcurrentHashMap<>();

  // TODO comment, decide what to make public.
  public static Object getSerializedCurrentContext() {
    return encodeContext(Tracing.getTracer().getCurrentSpan().getContext());
  }

  public static void storeCurrentContextAsParent(FlightMap inputMap) {
    inputMap.put(
        PARENT_SPAN_CONTEXT_MAP_KEY,
        encodeContext(Tracing.getTracer().getCurrentSpan().getContext()));
  }

  @Override
  public HookAction startFlight(FlightContext context) throws InterruptedException {
    SpanContext parentLinkContext = getOrCreateParentContext(context);
    Link parentLink = Link.fromSpanContext(parentLinkContext, Link.Type.PARENT_LINKED_SPAN);
    /// TODO stairway flight/span standard prefixes?
    Span flightSpan = Tracing.getTracer().spanBuilder(context.getFlightClassName()).startSpan();
    flightSpan.addLink(parentLink);
    flightSpan.putAttribute("flightId", AttributeValue.stringAttributeValue(context.getFlightId()));
    flightSpan.putAttribute(
        "flightClass", AttributeValue.stringAttributeValue(context.getFlightClassName()));
    flightSpans.put(context.getFlightId(), flightSpan);
    return HookAction.CONTINUE;
  }

  @Override
  public HookAction endFlight(FlightContext context) throws InterruptedException {
    Span flightSpan = flightSpans.remove(context.getFlightId());
    flightSpan.end();
    // TODO DO NOT SUBMIT make sure stairway always ends flights or the span will dangle.
    return HookAction.CONTINUE;
  }

  @Override
  public Optional<StepHook> stepFactory(FlightContext context) {
    return Optional.of(new TraceStepHook());
  }

  private class TraceStepHook implements StepHook {
    private Span stepSpan = null;
    private Scope stepScope = null;

    @Override
    public HookAction startStep(FlightContext flightContext) {
      Span flightSpan = flightSpans.get(flightContext.getFlightId());

      stepSpan =
          Tracing.getTracer()
              .spanBuilderWithExplicitParent(flightContext.getStepClassName(), flightSpan)
              .startSpan();
      stepSpan.putAttribute(
          "flightId", AttributeValue.stringAttributeValue(flightContext.getFlightId()));
      stepSpan.putAttribute(
          "flightClass", AttributeValue.stringAttributeValue(flightContext.getFlightClassName()));
      stepSpan.putAttribute(
          "stepClass", AttributeValue.stringAttributeValue(flightContext.getStepClassName()));

      stepScope = tracer.withSpan(stepSpan);
      logger.info(
          "start step. flight id {}\n flight span context {}\nstep span context {}",
          flightContext.getFlightId(),
          flightSpan.getContext().toString(),
          stepSpan.getContext().toString());
      return HookAction.CONTINUE;
    }

    @Override
    public HookAction endStep(FlightContext flightContext) {
      stepScope.close();
      stepSpan.end();
      return HookAction.CONTINUE;
    }
  }

  private SpanContext getOrCreateParentContext(FlightContext flightContext) {
    // Check the input parameters for a parent span to link for the flight.
    String encodedContext =
        flightContext.getInputParameters().get(PARENT_SPAN_CONTEXT_MAP_KEY, String.class);
    if (encodedContext != null) {
      return decodeContext(encodedContext);
    }
    // Check the working map for a parent span to link for the flight.
    encodedContext = flightContext.getWorkingMap().get(PARENT_SPAN_CONTEXT_MAP_KEY, String.class);
    if (encodedContext != null) {
      return decodeContext(encodedContext);
    }
    // Create a new span to use as a parent span to link for the flight.
    Span flightSpan =
        tracer.spanBuilderWithExplicitParent(flightContext.getFlightClassName(), null).startSpan();
    flightSpan.putAttribute(
        "flightId", AttributeValue.stringAttributeValue(flightContext.getFlightId()));
    flightSpan.putAttribute(
        "flightClass", AttributeValue.stringAttributeValue(flightContext.getFlightClassName()));

    SpanContext flightSpanContext = flightSpan.getContext();
    // Store the parent span so that there is only one of these per flight.
    flightContext
        .getWorkingMap()
        .put(PARENT_SPAN_CONTEXT_MAP_KEY, encodeContext(flightSpanContext));

    // End the span immediately. We cannot carry this span until the flight finishes, we merely use
    // it to be linked.
    flightSpan.end();
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
