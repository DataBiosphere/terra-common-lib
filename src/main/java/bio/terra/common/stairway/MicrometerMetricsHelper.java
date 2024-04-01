package bio.terra.common.stairway;

/*
import bio.terra.stairway.Direction;
import bio.terra.stairway.FlightStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

import java.time.Duration;
 */
public class MicrometerMetricsHelper {

  /*
  private static final CompositeMeterRegistry registry = Metrics.globalRegistry;

  /**
   * Record the latency for stairway flights.
   *
   * @param flightName
   * @param flightStatus
   * @param latency
   *
  @Override
  public void recordFlightLatency(String flightName, FlightStatus flightStatus, Duration latency) {
      var flightLatencyTimer =
              Timer.builder(FLIGHT_LATENCY_METER_NAME)
                      .description("Latency of Stairway flight")
                      .tag(FLIGHT_NAME, flightName)
                      .tag(FLIGHT_STATUS, flightStatus.name())
                      .publishPercentileHistogram()
                      // min and max? longtimer instead? phist overkill?
                      .register(registry);
      flightLatencyTimer.record(latency);
  }*/

  /**
   * Records the failed flights. TODO rename
   *
   * @param flightName
   * @param flightStatus @Override public void recordFlightError(String flightName, FlightStatus
   *     flightStatus) { Counter.builder(FLIGHT_RESULT_METER_NAME) .description("Completed Stairway
   *     flight") .tag(FLIGHT_NAME, flightName) .tag(FLIGHT_STATUS, flightStatus.name())
   *     .register(registry) .increment(); }
   */

  /**
   * Record the latency for stairway steps.
   *
   * @param flightName
   * @param stepDirection
   * @param stepName
   * @param latency @Override public void recordStepLatency(String flightName, Direction
   *     stepDirection, String stepName, Duration latency) { Timer.builder(STEP_LATENCY_METER_NAME)
   *     .description("Latency of Stairway step") .tag(FLIGHT_NAME, flightName) .tag(STEP_NAME,
   *     stepName) .tag(STEP_DIRECTION, stepDirection.name()) .publishPercentileHistogram() // min
   *     and max? longtimer instead? phist overkill? // TODO: configurable tags? configurable other
   *     things? .register(registry) .record(latency); }
   */

  /**
   * Records the failed steps.
   *
   * @param flightName
   * @param stepDirection
   * @param stepName @Override public void recordStepDirection(String flightName, Direction
   *     stepDirection, String stepName) { Counter.builder(STEP_RESULT_METER_NAME)
   *     .description("Completed Stairway step") .tag(FLIGHT_NAME, flightName) .tag(STEP_NAME,
   *     stepName) .tag(STEP_DIRECTION, stepDirection.name()) .register(registry) .increment(); }
   */
}
