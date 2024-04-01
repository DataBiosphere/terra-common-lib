package bio.terra.common.stairway;

import bio.terra.stairway.Direction;
import bio.terra.stairway.FlightStatus;
import io.opentelemetry.api.common.AttributeKey;
import java.time.Duration;

public interface IMetricsHelper {

  String METRICS_PREFIX = "terra/common-lib";
  String FLIGHT_LATENCY_METER_NAME = METRICS_PREFIX + "/stairway/flight/latency";
  String FLIGHT_RESULT_METER_NAME = METRICS_PREFIX + "/stairway/flight/result";
  String STEP_LATENCY_METER_NAME = METRICS_PREFIX + "/stairway/step/latency";
  String STEP_RESULT_METER_NAME = METRICS_PREFIX + "/stairway/step/result";
  String FLIGHT_NAME = "flight_name";
  String FLIGHT_STATUS = "flight_status";
  String STEP_NAME = "step_name";
  String STEP_DIRECTION = "step_direction";

  AttributeKey<String> KEY_FLIGHT_NAME = AttributeKey.stringKey("flight_name");
  AttributeKey<String> KEY_FLIGHT_STATUS = AttributeKey.stringKey("flight_status");
  AttributeKey<String> KEY_STEP_NAME = AttributeKey.stringKey("step_name");
  AttributeKey<String> KEY_STEP_DIRECTION = AttributeKey.stringKey("step_direction");
  AttributeKey<String> KEY_ERROR = AttributeKey.stringKey("error_code");

  /** Unit string for count. */
  String COUNT = "1";

  /** Unit string for millisecond. */
  String MILLISECOND = "ms";

  /** Record the latency for stairway flights. */
  void recordFlightLatency(String flightName, FlightStatus flightStatus, Duration latency);

  /** Records the failed flights. */
  void recordFlightResult(String flightName, FlightStatus flightStatus);

  /** Record the latency for stairway steps. */
  void recordStepLatency(
      String flightName, Direction stepDirection, String stepName, Duration latency);

  /** Records the failed steps. */
  void recordStepResult(String flightName, Direction stepDirection, String stepName);
}
