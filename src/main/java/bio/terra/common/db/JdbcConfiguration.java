package bio.terra.common.db;

import java.util.Properties;
import org.apache.commons.dbcp2.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/** Base class for accessing JDBC configuration properties. */
public class JdbcConfiguration {
  private final JdbcProperties jdbcProperties;

  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  public JdbcConfiguration(JdbcProperties jdbcProperties) {
    this.jdbcProperties = jdbcProperties;
  }

  public JdbcProperties getJdbcProperties() {
    return jdbcProperties;
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
    props.setProperty("user", jdbcProperties.getUsername());
    props.setProperty("password", jdbcProperties.getPassword());

    ConnectionFactory connectionFactory =
        new DriverManagerConnectionFactory(jdbcProperties.getUri(), props);

    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
    config.setJmxEnabled(jdbcProperties.isJmxEnabled());

    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory, config);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("uri", jdbcProperties.getUri())
        .append("username", jdbcProperties.getUsername())
        .toString();
  }
}
