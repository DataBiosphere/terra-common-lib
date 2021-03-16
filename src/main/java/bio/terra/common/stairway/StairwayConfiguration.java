package bio.terra.common.stairway;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = StairwayProperties.class)
public class StairwayConfiguration {

  private final StairwayProperties stairwayProperties;

  public StairwayConfiguration(StairwayProperties stairwayProperties) {

    this.stairwayProperties = stairwayProperties;
  }

  public boolean isForceCleanStart() {
    return stairwayProperties.isForceCleanStart();
  }

  public boolean isMigrateUpgrade() {
    return stairwayProperties.isMigrateUpgrade();
  }

  public int getMaxParallelFlights() {
    return stairwayProperties.getMaxParallelFlights();
  }

  public Duration getQuietDownTimeout() {
    return stairwayProperties.getQuietDownTimeout();
  }

  public Duration getTerminateTimeout() {
    return stairwayProperties.getTerminateTimeout();
  }
}
