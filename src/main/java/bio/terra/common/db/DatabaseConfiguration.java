package bio.terra.common.db;

import java.util.Properties;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/** Base class to config JDBC Data Source. */
public class DatabaseConfiguration {
  private final DatabaseProperties databaseProperties;

  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  public DatabaseConfiguration(DatabaseProperties databaseProperties) {
    this.databaseProperties = databaseProperties;
  }

  public DatabaseProperties getDatabaseProperties() {
    return databaseProperties;
  }
  // Main use of the configuration is this pooling data source object.
  public PoolingDataSource<PoolableConnection> getDataSource() {
    // Lazy allocation of the data source
    if (dataSource == null) {
      configureDataSource();
    }
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

    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory, config);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }
}
