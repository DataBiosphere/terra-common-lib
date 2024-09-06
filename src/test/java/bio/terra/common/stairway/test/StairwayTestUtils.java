package bio.terra.common.stairway.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.common.db.BaseDatabaseProperties;
import bio.terra.common.db.DataSourceInitializer;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.StairwayBuilder;
import bio.terra.stairway.StairwayMapper;
import bio.terra.stairway.exception.DatabaseOperationException;
import bio.terra.stairway.exception.StairwayException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;

/** Test utilities for testing integrations with {@link Stairway}. */
public class StairwayTestUtils {

  /** Use Stairway objectmapper. */
  private static final ObjectMapper OBJECT_MAPPER = StairwayMapper.getObjectMapper();

  /** Returns an initialized and started Stairway instance from the Stairway.Builder. */
  public static Stairway setupStairway(StairwayBuilder builder) {
    try {
      Stairway stairway = builder.build();
      stairway.initialize(
          makeDataSource(), /* forceCleanStart= */ true, /* migrateUpgrade= */ true);
      stairway.recoverAndStart(/* obsoleteStairways= */ null);
      return stairway;
    } catch (StairwayException | InterruptedException e) {
      throw new RuntimeException(
          "Unable to initialize Stairway for testing. Is the database on?", e);
    }
  }

  private static DataSource makeDataSource() {
    BaseDatabaseProperties databaseProperties = new BaseDatabaseProperties();
    databaseProperties.setUri(
        getEnvVar("STAIRWAY_URI", "jdbc:postgresql://127.0.0.1:5432/tclstairway"));
    databaseProperties.setUsername(getEnvVar("STAIRWAY_USERNAME", "tclstairwayuser"));
    databaseProperties.setPassword(getEnvVar("STAIRWAY_PASSWORD", "tclstairwaypwd"));
    return DataSourceInitializer.initializeDataSource(databaseProperties);
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

  public static <T> void validateJsonDeserialization(String json, T request)
      throws JsonProcessingException {
    T deserialized = OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  public static String serializeToJson(Object object) throws JsonProcessingException {
    return OBJECT_MAPPER.writeValueAsString(object);
  }
}
