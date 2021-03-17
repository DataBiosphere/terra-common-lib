package bio.terra.common.db;

import java.util.Properties;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableConfigurationProperties(value = JdbcProperties.class)
public class JdbcConfig {

  private final BaseJdbcProperties jdbcProperties;
  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  public JdbcConfig(BaseJdbcProperties jdbcProperties) {
    this.jdbcProperties = jdbcProperties;
  }

  // Main use of the configuration is this pooling data source object.
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
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

    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory);

    poolableConnectionFactory.setPool(connectionPool);

    dataSource = new PoolingDataSource<>(connectionPool);
  }

  @Bean("transactionManager")
  public PlatformTransactionManager getTransactionManager() {
    return new DataSourceTransactionManager(getDataSource());
  }
}
