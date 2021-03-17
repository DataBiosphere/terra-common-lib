package bio.terra.common.stairway;

import bio.terra.common.db.BaseJdbcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

// TODO: merge with StairwayProperties & kill inheritance
@ConfigurationProperties(prefix = "terra.common.stairway.db")
public class StairwayJdbcProperties extends BaseJdbcProperties {}
