package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsTestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.stairway.Direction;
import bio.terra.stairway.FlightStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MetricsHelperTest {
  private static final Duration METRICS_COLLECTION_INTERVAL = Duration.ofMillis(10);
  private static final String FAKE_FLIGHT_NAME = "fakeFlight";
  private static final FlightStatus FAKE_FLIGHT_STATUS_NAME = FlightStatus.SUCCESS;
  private static final String FAKE_STEP_NAME = "fakeStep";

  private MetricsHelper metricsHelper;
  private TestMetricExporter testMetricExporter;

  @BeforeEach
  void setup() {
    testMetricExporter = new TestMetricExporter();
    metricsHelper = new MetricsHelper(openTelemetry(testMetricExporter));
  }

  @Test
  void recordErrorCount() {
    metricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    metricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    metricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    metricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.FATAL);
    metricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.SUCCESS);

    var metric = waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL);

    assertEquals(FLIGHT_ERROR_METER_NAME, metric.getName());
    assertFlightErrorMeterValues(
        metric, Map.of(FlightStatus.ERROR, 3L, FlightStatus.FATAL, 1L, FlightStatus.SUCCESS, 1L));
  }

  @Test
  void recordFlightLatency() {
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(1));
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(1));
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(0));

    var metric = waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL);

    assertEquals(FLIGHT_LATENCY_METER_NAME, metric.getName());
    assertLatencyBucketCounts(metric, Map.of(0, 1L, 1, 2L));
  }

  @Test
  void recordStepLatency() {
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(1));
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(1));
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(0));

    var metric = waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL);

    assertEquals(STEP_LATENCY_METER_NAME, metric.getName());
    assertLatencyBucketCounts(metric, Map.of(0, 1L, 1, 2L));
  }

  @Test
  void recordStepErrorCount() {
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);

    var metric = waitForMetrics(testMetricExporter, METRICS_COLLECTION_INTERVAL);

    assertEquals(STEP_ERROR_METER_NAME, metric.getName());
    assertStepErrorMeterValues(
        metric, FAKE_STEP_NAME, Map.of(Direction.DO, 2L, Direction.UNDO, 3L));
  }

  public OpenTelemetry openTelemetry(TestMetricExporter testMetricExporter) {
    var sdkMeterProviderBuilder =
        SdkMeterProvider.builder()
            .registerMetricReader(
                PeriodicMetricReader.builder(testMetricExporter)
                    .setInterval(METRICS_COLLECTION_INTERVAL)
                    .build());

    return OpenTelemetrySdk.builder().setMeterProvider(sdkMeterProviderBuilder.build()).build();
  }
}
