package bio.terra.common.db;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Base class to config JDBC data source. Client can entend along with customized {@link
 * DatabaseProperties} to build their data source. See {@link
 * bio.terra.common.stairway.StairwayDatabaseConfiguration} to understand how to use it in SpringBoot.
 */
public class DatabaseConfiguration {
  private final DatabaseProperties databaseProperties;

  // Not a property
  private DataSource dataSource;

  public DatabaseConfiguration(DatabaseProperties databaseProperties) {
    this.databaseProperties = databaseProperties;
    configureDataSource();
  }

  public DatabaseProperties getDatabaseProperties() {
    return databaseProperties;
  }
  // Main use of the configuration is this pooling data source object.
  public DataSource getDataSource() {
    return dataSource;
  }

  private void configureDataSource() {
    Properties props = new Properties();
    props.setProperty("user", databaseProperties.getUsername());
    props.setProperty("password", databaseProperties.getPassword());

    ConnectionFactory connectionFactory =
        new DriverManagerConnectionFactory(databaseProperties.getUri(), props);

    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
    config.setJmxEnabled(databaseProperties.isJmxEnabled());
    config.setMaxTotal(databaseProperties.getPoolMaxTotal());
    config.setMaxIdle(databaseProperties.getPoolMaxIdle());
    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory, config);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }
}
