package bio.terra.common.stairway;

import bio.terra.common.db.BaseDatabaseProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terra.common.stairway.db")
public class StairwayDatabaseProperties extends BaseDatabaseProperties {}
