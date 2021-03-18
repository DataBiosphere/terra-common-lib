package bio.terra.common.db;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Base class to config DataBase data source. Client can entend along with customized {@link
 * BaseDatabaseProperties} to build their data source. See {@link
 * bio.terra.common.stairway.StairwayDatabaseConfiguration} to understand how to use it in SpringBoot.
 */
public abstract class BaseDatabaseConfiguration {
  private final BaseDatabaseProperties baseDatabaseProperties;

  // Not a property
  private DataSource dataSource;

  public BaseDatabaseConfiguration(BaseDatabaseProperties baseDatabaseProperties) {
    this.baseDatabaseProperties = baseDatabaseProperties;
    configureDataSource();
  }

  public BaseDatabaseProperties getBaseDatabaseProperties() {
    return baseDatabaseProperties;
  }
  // Main use of the configuration is this pooling data source object.
  public DataSource getDataSource() {
    return dataSource;
  }

  private void configureDataSource() {
    Properties props = new Properties();
    props.setProperty("user", baseDatabaseProperties.getUsername());
    props.setProperty("password", baseDatabaseProperties.getPassword());

    ConnectionFactory connectionFactory =
        new DriverManagerConnectionFactory(baseDatabaseProperties.getUri(), props);

    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
    config.setJmxEnabled(baseDatabaseProperties.isJmxEnabled());
    config.setMaxTotal(baseDatabaseProperties.getPoolMaxTotal());
    config.setMaxIdle(baseDatabaseProperties.getPoolMaxIdle());
    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory, config);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }
}
