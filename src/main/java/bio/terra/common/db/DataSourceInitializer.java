package bio.terra.common.db;

import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/** Utility class to create {@link DataSource}. */
public class DataSourceInitializer {
  private DataSourceInitializer() {}

  /** Create a {@link DataSource} by providing a {@link BaseDatabaseProperties}. */
  public static DataSource initializeDataSource(BaseDatabaseProperties baseDatabaseProperties) {
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

    return new PoolingDataSource<>(connectionPool);
  }
}
