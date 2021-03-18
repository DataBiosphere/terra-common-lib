package bio.terra.common.stairway;

import bio.terra.common.db.BaseDatabaseConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = StairwayDatabaseProperties.class)
public class StairwayDatabaseConfiguration extends BaseDatabaseConfiguration {
  public StairwayDatabaseConfiguration(StairwayDatabaseProperties stairwayDatabaseProperties) {
    super(stairwayDatabaseProperties);
  }
}
