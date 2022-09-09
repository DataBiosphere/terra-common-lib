package bio.terra.common.db;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Utility class to create {@link DataSource}.
 *
 * Prefer using {@DataSourceManager} instead of this code so that connection pools are
 * closed when the Spring application context is deleted.
 */
public class DataSourceInitializer {
  private DataSourceInitializer() {}
  ;

  /** Create a {@link DataSource} by providing a {@link BaseDatabaseProperties}. */
  public static DataSource initializeDataSource(BaseDatabaseProperties baseDatabaseProperties) {
    ObjectPool<PoolableConnection> connectionPool = makeConnectionPool(baseDatabaseProperties);
    return new PoolingDataSource<>(connectionPool);
  }

  /**
   * This method is shared by this class and the DataSourceManager so we only have one implementation
   * of creating the connection pool
   *
   * @param baseDatabaseProperties common database properties
   * @return connection pool
   */
  public static ObjectPool<PoolableConnection> makeConnectionPool(BaseDatabaseProperties baseDatabaseProperties) {
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
    return connectionPool;
  }
}
