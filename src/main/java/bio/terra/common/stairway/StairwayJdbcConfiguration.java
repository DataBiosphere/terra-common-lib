package bio.terra.common.stairway;

import bio.terra.common.db.JdbcConfiguration;
import bio.terra.common.db.JdbcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = StairwayJdbcProperties.class)
public class StairwayJdbcConfiguration extends JdbcConfiguration {
  public StairwayJdbcConfiguration(JdbcProperties jdbcProperties) {
    super(jdbcProperties);
  }
}
