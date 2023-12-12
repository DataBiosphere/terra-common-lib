package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsTestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.common.stairway.test.StairwayTestUtils;
import bio.terra.stairway.Direction;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.StairwayBuilder;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class MonitoringHookTest {
  private static final Duration METRICS_COLLECTION_INTERVAL = Duration.ofMillis(10);
  private OpenTelemetry openTelemetry;
  private TestMetricExporter testMetricExporter;

  @BeforeEach
  void setup() {
    testMetricExporter = new TestMetricExporter();
    openTelemetry = openTelemetry(testMetricExporter);
  }

  public OpenTelemetry openTelemetry(TestMetricExporter testMetricExporter) {
    var sdkMeterProviderBuilder =
        SdkMeterProvider.builder()
            .registerMetricReader(
                PeriodicMetricReader.builder(testMetricExporter)
                    .setInterval(METRICS_COLLECTION_INTERVAL)
                    .build());

    var propagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance());
    return OpenTelemetrySdk.builder()
        .setPropagators(propagators)
        .setMeterProvider(sdkMeterProviderBuilder.build())
        .build();
  }

  @Test
  public void spansConnected() throws Exception {
    var instrumenter =
        Instrumenter.<String, Object>builder(openTelemetry, "test", name -> name)
            .buildInstrumenter();
    var parentContext = instrumenter.start(Context.current(), "parent");

    List<SpanContext> contextRecord = new ArrayList<>();
    RecordContextStep.setRecord(contextRecord);

    try (var ignored = parentContext.makeCurrent()) {
      contextRecord.add(Span.current().getSpanContext());
      var inputMap = new FlightMap();
      MonitoringHook.storeCurrentContextAsSubmission(inputMap, openTelemetry);

      Stairway stairway =
          StairwayTestUtils.setupStairway(
              new StairwayBuilder().stairwayHook(new MonitoringHook(openTelemetry)));
      FlightState flightState =
          StairwayTestUtils.blockUntilFlightCompletes(
              stairway, SpanRecordingFlight.class, inputMap, Duration.ofSeconds(5));
      assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());
    } finally {
      instrumenter.end(parentContext, "parent", null, null);
    }

    // There should be 3 recorded Spans, one for each of the 2 steps and 1 parent.
    assertThat(contextRecord, Matchers.hasSize(3));
    assertSharedTraceId(contextRecord);
    assertAllDifferentSpanIds(contextRecord);
  }

  @Test
  void recordMetricsOnSuccess() throws Exception {
    Stairway stairway =
        StairwayTestUtils.setupStairway(
            new StairwayBuilder().stairwayHook(new MonitoringHook(openTelemetry)));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, SpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    var metricsByName =
        waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL, 4).stream()
            .collect(Collectors.toMap(MetricData::getName, Function.identity()));

    assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());
    assertEquals(
        Set.of(
            FLIGHT_LATENCY_METER_NAME,
            FLIGHT_ERROR_METER_NAME,
            STEP_ERROR_METER_NAME,
            STEP_LATENCY_METER_NAME),
        metricsByName.keySet());

    assertFlightErrorMeterValues(
        metricsByName.get(FLIGHT_ERROR_METER_NAME), Map.of(FlightStatus.SUCCESS, 1L));
    assertStepErrorMeterValues(
        metricsByName.get(STEP_ERROR_METER_NAME),
        RecordContextStep.class.getName(),
        Map.of(Direction.DO, 2L));
    assertLatencyTotalCount(metricsByName.get(FLIGHT_LATENCY_METER_NAME), 1L);
    assertLatencyTotalCount(metricsByName.get(STEP_LATENCY_METER_NAME), 2L);
  }

  /** A {@link Flight} with two steps for recording the span context. */
  public static class SpanRecordingFlight extends Flight {
    public SpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
      super(inputParameters, applicationContext);
      addStep(new RecordContextStep());
      addStep(new RecordContextStep());
    }
  }

  @Test
  public void undoSpansOnFailure() throws Exception {
    List<SpanContext> contextRecord = new ArrayList<>();
    RecordContextStep.setRecord(contextRecord);

    Stairway stairway =
        StairwayTestUtils.setupStairway(
            new StairwayBuilder().stairwayHook(new MonitoringHook(openTelemetry)));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));
    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());
    // There are 4 recorded spans, 2 for each do and undo of the RecordContextStep execution.
    assertThat(contextRecord, Matchers.hasSize(4));
    assertSharedTraceId(contextRecord);
    assertAllDifferentSpanIds(contextRecord);
  }

  @Test
  void recordMetricsOnError() throws Exception {
    Stairway stairway =
        StairwayTestUtils.setupStairway(
            new StairwayBuilder().stairwayHook(new MonitoringHook(openTelemetry)));

    var flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());

    var metricsByName =
        waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL, 4).stream()
            .collect(Collectors.toMap(MetricData::getName, Function.identity()));

    assertEquals(
        Set.of(
            FLIGHT_LATENCY_METER_NAME,
            FLIGHT_ERROR_METER_NAME,
            STEP_ERROR_METER_NAME,
            STEP_LATENCY_METER_NAME),
        metricsByName.keySet());

    assertFlightErrorMeterValues(
        metricsByName.get(FLIGHT_ERROR_METER_NAME), Map.of(FlightStatus.ERROR, 1L));

    assertStepErrorMeterValues(
        metricsByName.get(STEP_ERROR_METER_NAME),
        RecordContextStep.class.getName(),
        Map.of(Direction.DO, 2L, Direction.UNDO, 2L));
    assertStepErrorMeterValues(
        metricsByName.get(STEP_ERROR_METER_NAME),
        FailureStep.class.getName(),
        Map.of(Direction.DO, 1L, Direction.SWITCH, 1L));
    assertLatencyTotalCount(metricsByName.get(FLIGHT_LATENCY_METER_NAME), 1L);
    assertStepLatencyTotalCount(
        metricsByName.get(STEP_LATENCY_METER_NAME),
        RecordContextStep.class.getName(),
        Direction.DO,
        2L);
    assertStepLatencyTotalCount(
        metricsByName.get(STEP_LATENCY_METER_NAME),
        RecordContextStep.class.getName(),
        Direction.UNDO,
        2L);
    assertStepLatencyTotalCount(
        metricsByName.get(STEP_LATENCY_METER_NAME), FailureStep.class.getName(), Direction.DO, 1L);
    assertStepLatencyTotalCount(
        metricsByName.get(STEP_LATENCY_METER_NAME),
        FailureStep.class.getName(),
        Direction.SWITCH,
        1L);
  }

  private static void assertSharedTraceId(List<SpanContext> contextRecord) {
    assertEquals(1, contextRecord.stream().map(SpanContext::getTraceId).distinct().count());
  }

  private static void assertAllDifferentSpanIds(List<SpanContext> contextRecord) {
    assertEquals(
        contextRecord.size(),
        contextRecord.stream().map(SpanContext::getSpanId).distinct().count());
  }

  /** A {@link Flight} with two steps for recording the span context and a {@link FailureStep}. */
  public static class ErrorSpanRecordingFlight extends Flight {
    public ErrorSpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
      super(inputParameters, applicationContext);
      addStep(new RecordContextStep());
      addStep(new RecordContextStep());
      addStep(new FailureStep());
    }
  }

  /** A {@link Step} that records the current span within each step execution. */
  public static class RecordContextStep implements Step {
    /** The current list within which to record step execution spans. */
    private static List<SpanContext> contextRecord;

    /** Set a new global list to append to for every {@link SpanContext} seen by these steps. */
    public static void setRecord(List<SpanContext> newRecord) {
      contextRecord = newRecord;
    }

    @Override
    public StepResult doStep(FlightContext flightContext) {
      record(Span.current().getSpanContext());
      return StepResult.getStepResultSuccess();
    }

    @Override
    public StepResult undoStep(FlightContext flightContext) {
      record(Span.current().getSpanContext());
      return StepResult.getStepResultSuccess();
    }

    private static void record(SpanContext context) {
      if (contextRecord != null) {
        contextRecord.add(context);
      }
    }
  }

  /** A {@link Step} that always fails. */
  public static class FailureStep implements Step {
    @Override
    public StepResult doStep(FlightContext flightContext) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL);
    }

    @Override
    public StepResult undoStep(FlightContext flightContext) {
      return StepResult.getStepResultSuccess();
    }
  }
}
