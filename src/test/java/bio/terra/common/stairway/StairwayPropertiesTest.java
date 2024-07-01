package bio.terra.common.stairway;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.stairway.DefaultThreadPoolTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Tag("unit")
public class StairwayPropertiesTest {

  private StairwayProperties properties;

  @BeforeEach
  void beforeEach() {
    this.properties = new StairwayProperties();
  }

  @Test
  void testStairwayProperties_executor_maxParallelFlightsUnspecified() {
    assertThat(
        "maxParallelFlights are 0 when unspecified",
        properties.getMaxParallelFlights(),
        equalTo(0));

    ThreadPoolTaskExecutor executor = properties.stairwayExecutor();
    assertThat(executor, instanceOf(DefaultThreadPoolTaskExecutor.class));
    assertThat(
        "DefaultThreadPoolTaskExecutor overrides invalid pool size",
        executor.getMaxPoolSize(),
        greaterThan(0));
    assertTrue(executor.isRunning());
  }

  @Test
  void testStairwayProperties_executor_maxParallelFlightsSpecified() {
    int maxParallelFlights = 24;
    properties.setMaxParallelFlights(maxParallelFlights);
    assertThat(properties.getMaxParallelFlights(), equalTo(maxParallelFlights));

    ThreadPoolTaskExecutor executor = properties.stairwayExecutor();
    assertThat(executor, instanceOf(DefaultThreadPoolTaskExecutor.class));
    assertThat(
        "DefaultThreadPoolTaskExecutor honors valid pool size",
        executor.getMaxPoolSize(),
        equalTo(maxParallelFlights));
    assertTrue(executor.isRunning());
  }
}
