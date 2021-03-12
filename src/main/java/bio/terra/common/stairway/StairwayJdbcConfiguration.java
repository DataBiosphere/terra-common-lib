package bio.terra.common.stairway;

import bio.terra.common.db.JdbcConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "buffer.stairway.db")
public class StairwayJdbcConfiguration extends JdbcConfiguration {}
