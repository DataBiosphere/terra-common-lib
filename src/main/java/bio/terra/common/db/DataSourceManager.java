package bio.terra.common.db;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class initializes data source pools just like {@link DataSourceInitializer}, but it
 * remembers the pools that it creates. It uses a @PreDestroy method to close the pools when the
 * Spring application context is closed.
 *
 * <p>Using this class is important when running JUnit tests that use @DirtiesContext directives.
 * Without this extra step, when each dirty context is deleted, connections in its pools are
 * orphaned. Use of the class has no effect in non-test environments.
 */
@Component
public class DataSourceManager {
  private static final Logger logger = LoggerFactory.getLogger(DataSourceManager.class);
  private List<ObjectPool<PoolableConnection>> connectionPools = new ArrayList<>();

  @PreDestroy
  public void destroy() {
    logger.info("Closing all connection pools");
    for (var pool : connectionPools) {
      logger.info("Pool active {} idle {}", pool.getNumActive(), pool.getNumIdle());
      pool.close();
    }
    connectionPools = new ArrayList<>();
  }

  public DataSource initializeDataSource(BaseDatabaseProperties baseDatabaseProperties) {
    ObjectPool<PoolableConnection> connectionPool =
        DataSourceInitializer.makeConnectionPool(baseDatabaseProperties);
    connectionPools.add(connectionPool);
    return new PoolingDataSource<>(connectionPool);
  }
}
