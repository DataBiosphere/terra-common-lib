package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_LATENCY_METER_NAME;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.stairway.Direction;
import bio.terra.stairway.FlightStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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

    var metric = waitForMetrics();

    assertEquals(FLIGHT_ERROR_METER_NAME, metric.getName());
    var valuesByError =
        metric.getData().getPoints().stream()
            .collect(
                Collectors.toMap(
                    point -> point.getAttributes().get(MetricsHelper.KEY_ERROR),
                    point -> ((LongPointData) point).getValue()));

    assertEquals(valuesByError.get(FlightStatus.ERROR.name()), 3);
    assertEquals(valuesByError.get(FlightStatus.FATAL.name()), 1);
    assertEquals(valuesByError.get(FlightStatus.SUCCESS.name()), 1);
  }

  private MetricData waitForMetrics() {
    await()
        .atMost(1, TimeUnit.SECONDS)
        .pollInterval(METRICS_COLLECTION_INTERVAL)
        .until(() -> testMetricExporter.getLastMetrics() != null);
    var lastMetrics = testMetricExporter.getLastMetrics();
    assertEquals(1, lastMetrics.size());
    return lastMetrics.iterator().next();
  }

  @Test
  void RecordFlightLatency() {
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(1));
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(1));
    metricsHelper.recordFlightLatency(
        FAKE_FLIGHT_NAME, FAKE_FLIGHT_STATUS_NAME, Duration.ofMillis(0));

    var metric = waitForMetrics();

    assertEquals(FLIGHT_LATENCY_METER_NAME, metric.getName());
    assertEquals(1, metric.getData().getPoints().size());
    var point = (HistogramPointData) metric.getData().getPoints().iterator().next();
    assertEquals(1, point.getCounts().get(0));
    assertEquals(2, point.getCounts().get(1));
  }

  @Test
  void recordStepLatency() {
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(1));
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(1));
    metricsHelper.recordStepLatency(
        FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME, Duration.ofMillis(0));

    var metric = waitForMetrics();

    assertEquals(STEP_LATENCY_METER_NAME, metric.getName());
    assertEquals(1, metric.getData().getPoints().size());
    var point = (HistogramPointData) metric.getData().getPoints().iterator().next();
    assertEquals(1, point.getCounts().get(0));
    assertEquals(2, point.getCounts().get(1));
  }

  @Test
  void recordStepErrorCount() {
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.DO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);
    metricsHelper.recordStepDirection(FAKE_FLIGHT_NAME, Direction.UNDO, FAKE_STEP_NAME);

    var metric = waitForMetrics();

    assertEquals(STEP_ERROR_METER_NAME, metric.getName());
    var valuesByStepDirection =
        metric.getData().getPoints().stream()
            .collect(
                Collectors.toMap(
                    point ->
                        Direction.valueOf(
                            point.getAttributes().get(MetricsHelper.KEY_STEP_DIRECTION)),
                    point -> ((LongPointData) point).getValue()));

    assertEquals(valuesByStepDirection.get(Direction.DO), 2);
    assertEquals(valuesByStepDirection.get(Direction.UNDO), 3);
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
