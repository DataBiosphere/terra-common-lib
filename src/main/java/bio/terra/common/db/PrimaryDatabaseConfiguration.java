package bio.terra.common.db;

import bio.terra.common.stairway.StairwayDatabaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Pre-defined {@link BaseDatabaseConfiguration} that can be used directly to config data source. */
@Configuration
@EnableConfigurationProperties(value = PrimaryDatabaseProperties.class)
public class PrimaryDatabaseConfiguration extends BaseDatabaseConfiguration {
  public PrimaryDatabaseConfiguration(PrimaryDatabaseProperties primaryDatabaseProperties) {
    super(primaryDatabaseProperties);
  }
}
