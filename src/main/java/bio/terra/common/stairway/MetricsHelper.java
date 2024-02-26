package bio.terra.common.stairway;

import bio.terra.stairway.Direction;
import bio.terra.stairway.FlightStatus;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.sdk.metrics.Aggregation;
import java.time.Duration;
import java.util.List;

public class MetricsHelper {
  public static final String METRICS_PREFIX = "terra/common-lib";
  public static final String FLIGHT_LATENCY_METER_NAME =
      METRICS_PREFIX + "/stairway/flight/latency";
  public static final String FLIGHT_ERROR_METER_NAME = METRICS_PREFIX + "/stairway/flight/error";
  public static final String STEP_LATENCY_METER_NAME = METRICS_PREFIX + "/stairway/step/latency";
  public static final String STEP_ERROR_METER_NAME = METRICS_PREFIX + "/stairway/step/error";
  public static final AttributeKey<String> KEY_FLIGHT_NAME = AttributeKey.stringKey("flight_name");
  public static final AttributeKey<String> KEY_FLIGHT_STATUS =
      AttributeKey.stringKey("flight_status");
  public static final AttributeKey<String> KEY_STEP_NAME = AttributeKey.stringKey("step_name");
  public static final AttributeKey<String> KEY_STEP_DIRECTION =
      AttributeKey.stringKey("step_direction");
  public static final AttributeKey<String> KEY_ERROR = AttributeKey.stringKey("error_code");

  /** Unit string for count. */
  private static final String COUNT = "1";

  /** Unit string for millisecond. */
  private static final String MILLISECOND = "ms";

  public static final Aggregation LATENCY_DISTRIBUTION =
      Aggregation.explicitBucketHistogram(
          List.of(
              0.0, 1.0, 2.0, 5.0, 10.0, 20.0, 40.0, 60.0, 80.0, 100.0, 120.0, 140.0, 160.0, 180.0,
              200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0, 2000.0, 4000.0,
              8000.0, 16000.0, 32000.0, 64000.0));

  public static final Aggregation COUNT_AGGREGATION = Aggregation.sum();

  /** Gauge for flight latency in milliseconds. */
  private final LongHistogram flightLatencyHistogram;

  /** Counter for number of errors from stairway flights. */
  private final LongCounter flightErrorCounter;

  /** Gauge for step latency in milliseconds. */
  private final LongHistogram stepLatencyHistogram;

  /** Counter for number of errors from stairway steps. */
  private final LongCounter stepErrorCounter;

  public MetricsHelper(OpenTelemetry openTelemetry) {
    var meter = openTelemetry.getMeter(MetricsHelper.class.getName());
    flightLatencyHistogram =
        meter
            .histogramBuilder(FLIGHT_LATENCY_METER_NAME)
            .setDescription("Latency for stairway flight")
            .setUnit(MILLISECOND)
            .ofLongs()
            .build();
    flightErrorCounter =
        meter
            .counterBuilder(FLIGHT_ERROR_METER_NAME)
            .setDescription("Number of stairway errors")
            .setUnit(COUNT)
            .build();
    stepLatencyHistogram =
        meter
            .histogramBuilder(STEP_LATENCY_METER_NAME)
            .setDescription("Latency for stairway step")
            .setUnit(MILLISECOND)
            .ofLongs()
            .build();
    stepErrorCounter =
        meter
            .counterBuilder(STEP_ERROR_METER_NAME)
            .setDescription("Number of stairway step errors")
            .setUnit(COUNT)
            .build();
  }

  /** Record the latency for stairway flights. */
  public void recordFlightLatency(String flightName, FlightStatus flightStatus, Duration latency) {
    Attributes attributes =
        Attributes.of(KEY_FLIGHT_NAME, flightName, KEY_FLIGHT_STATUS, flightStatus.name());

    flightLatencyHistogram.record(latency.toMillis(), attributes);
  }

  /** Records the failed flights. */
  public void recordFlightError(String flightName, FlightStatus flightStatus) {
    Attributes attributes =
        Attributes.of(KEY_ERROR, flightStatus.name(), KEY_FLIGHT_NAME, flightName);
    flightErrorCounter.add(1, attributes);
  }

  /** Record the latency for stairway flights. */
  public void recordStepLatency(
      String flightName, Direction stepDirection, String stepName, Duration latency) {
    Attributes attributes =
        Attributes.of(
            KEY_FLIGHT_NAME, flightName,
            KEY_STEP_DIRECTION, stepDirection.name(),
            KEY_STEP_NAME, stepName);

    stepLatencyHistogram.record(latency.toMillis(), attributes);
  }

  /** Records the failed flights. */
  public void recordStepDirection(String flightName, Direction stepDirection, String stepName) {
    Attributes attributes =
        Attributes.of(
            KEY_FLIGHT_NAME, flightName,
            KEY_STEP_DIRECTION, stepDirection.name(),
            KEY_STEP_NAME, stepName);

    stepErrorCounter.add(1, attributes);
  }
}
