package bio.terra.common.stairway.test;

import bio.terra.common.stairway.StairwayDatabaseConfiguration;
import bio.terra.common.stairway.StairwayDatabaseProperties;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.exception.DatabaseOperationException;
import bio.terra.stairway.exception.StairwayException;
import java.time.Duration;
import java.time.Instant;

/** Test utilities for testing integrations with {@link Stairway}. */
public class StairwayTestUtils {

  /** Returns an initialized and started Stairway instance from the Stairway.Builder. */
  public static Stairway setupStairway(Stairway.Builder builder) {
    try {
      Stairway stairway = builder.build();
      StairwayDatabaseConfiguration dbConfiguration = makeDbConfiguration();
      stairway.initialize(
          dbConfiguration.getDataSource(),
          dbConfiguration.getBaseDatabaseProperties().isInitializeOnStart(),
          dbConfiguration.getBaseDatabaseProperties().isUpgradeOnStart());
      stairway.recoverAndStart(/* obsoleteStairways =*/ null);
      return stairway;
    } catch (StairwayException | InterruptedException e) {
      throw new RuntimeException(
          "Unable to initialize Stairway for testing. Is the database on?", e);
    }
  }

  private static StairwayDatabaseConfiguration makeDbConfiguration() {
    StairwayDatabaseProperties databaseProperties = new StairwayDatabaseProperties();
    databaseProperties.setUri("jdbc:postgresql://127.0.0.1:5432/tclstairway");
    databaseProperties.setUsername("tclstairwayuser");
    databaseProperties.setPassword("tclstairwaypwd");
    databaseProperties.setInitializeOnStart(true);
    databaseProperties.setUpgradeOnStart(true);
    return new StairwayDatabaseConfiguration(databaseProperties);
  }

  private static String getEnvVar(String name, String defaultValue) {
    String value = System.getenv(name);
    return (value == null) ? defaultValue : value;
  }

  /**
   * Submits the flight and block until Stairway completes it by polling regularly until the timeout
   * is reached.
   */
  public static FlightState blockUntilFlightCompletes(
      Stairway stairway,
      Class<? extends Flight> flightClass,
      FlightMap inputParameters,
      Duration timeout)
      throws StairwayException, InterruptedException {
    String flightId = stairway.createFlightId();
    stairway.submit(flightId, flightClass, inputParameters);
    return pollUntilComplete(flightId, stairway, timeout.dividedBy(20), timeout);
  }

  /**
   * Polls stairway until the flight for {@code flightId} completes, or this has polled {@code
   * numPolls} times every {@code pollInterval}.
   */
  public static FlightState pollUntilComplete(
      String flightId, Stairway stairway, Duration pollInterval, Duration timeout)
      throws InterruptedException, DatabaseOperationException {
    for (Instant deadline = Instant.now().plus(timeout);
        Instant.now().isBefore(deadline);
        Thread.sleep(pollInterval.toMillis())) {
      FlightState flightState = stairway.getFlightState(flightId);
      if (!flightState.isActive()) {
        return flightState;
      }
    }
    throw new InterruptedException(
        String.format("Flight [%s] did not complete in the allowed wait time.", flightId));
  }
}
