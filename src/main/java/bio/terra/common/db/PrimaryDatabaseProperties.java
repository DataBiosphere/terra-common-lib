package bio.terra.common.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terra.common.db")
public class PrimaryDatabaseProperties extends BaseDatabaseProperties {}
