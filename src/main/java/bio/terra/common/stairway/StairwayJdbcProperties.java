package bio.terra.common.stairway;

import bio.terra.common.db.JdbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terra.common.stairway.db")
public class StairwayJdbcProperties extends JdbcProperties {}
