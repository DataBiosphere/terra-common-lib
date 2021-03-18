package bio.terra.common.stairway;

import bio.terra.common.db.DatabaseConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = StairwayDatabaseProperties.class)
public class StairwayDatabaseConfiguration extends DatabaseConfiguration {
  public StairwayDatabaseConfiguration(StairwayDatabaseProperties StairwayJdbcProperties) {
    super(StairwayJdbcProperties);
  }
}
