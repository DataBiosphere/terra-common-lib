package bio.terra.common.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Pre-defined {@link BaseDatabaseProperties} that configs one database configuration properties. */
@ConfigurationProperties(prefix = "terra.common.db")
public class PrimaryDatabaseProperties extends BaseDatabaseProperties {}
