package bio.terra.common.db;

import static org.apache.commons.pool2.impl.GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

import org.apache.commons.dbcp2.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/** Defines configuration properties for using {@link DatabaseConfiguration} use. */
public class DatabaseProperties {
  private boolean jmxEnabled = true;
  private String uri;
  private String username;
  private String password;

  // If true, the database will be wiped when start
  private boolean initializeOnStart;
  // If true, the database will have changesets applied when start
  private boolean upgradeOnStart;

  // Maximum number of database connections in the connection pool; -1 means no limit The goal of
  // these parameters is to prevent waiting for a database connection.
  private int poolMaxTotal = DEFAULT_MAX_TOTAL;
  // Maximum number of database connections to keep idle.
  private int poolMaxIdle = DEFAULT_MAX_TOTAL;

  // Not a property
  private PoolingDataSource<PoolableConnection> dataSource;

  /**
   * Returns a boolean indicating whether JMX should be enabled when creating connection pools. If
   * this is enabled, references to connection pools created at context initialization time will be
   * placed into an MBean scoped for the lifetime of the JVM. This is OK in production, but can be
   * an issue for tests that use @DirtiesContext and thus repeatedly initialize connection pools, as
   * these pools will hold active DB connections while held by JMX. Having this configuration allows
   * us to disable JMX in test environments. See PF-485 for more details.
   */
  public boolean isJmxEnabled() {
    return jmxEnabled;
  }

  public void setJmxEnabled(boolean jmxEnabled) {
    this.jmxEnabled = jmxEnabled;
  }

  public String getUri() {
    return uri;
  }

  // NOTE: even though the setters appear unused, the Spring infrastructure uses them to populate
  // the properties.
  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public boolean isInitializeOnStart() {
    return initializeOnStart;
  }

  public void setInitializeOnStart(boolean initializeOnStart) {
    this.initializeOnStart = initializeOnStart;
  }

  public boolean isUpgradeOnStart() {
    return upgradeOnStart;
  }

  public void setUpgradeOnStart(boolean upgradeOnStart) {
    this.upgradeOnStart = upgradeOnStart;
  }

  public int getPoolMaxTotal() {
    return poolMaxTotal;
  }

  public void setPoolMaxTotal(int poolMaxTotal) {
    this.poolMaxTotal = poolMaxTotal;
  }

  public int getPoolMaxIdle() {
    return poolMaxIdle;
  }

  public void setPoolMaxIdle(int poolMaxIdle) {
    this.poolMaxIdle = poolMaxIdle;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("uri", uri)
        .append("username", username)
        .append("initializeOnStart", initializeOnStart)
        .append("upgradeOnStart", upgradeOnStart)
        .append("jmxEnabled", jmxEnabled)
        .append("poolMaxTotal", poolMaxTotal)
        .append("poolMaxIdle", poolMaxIdle)
        .toString();
  }
}
