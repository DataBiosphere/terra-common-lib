package bio.terra.common.db;

import static org.apache.commons.pool2.impl.GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * The base class that defines configuration properties for {@link DataSourceInitializer} use.
 *
 * <p>Extend this if they want to define their own SpringBoot configuration properties.
 */
public class BaseDatabaseProperties {
  private boolean jmxEnabled = true;
  private String uri;
  private String username;
  private String password;

  // Maximum number of database connections in the connection pool; -1 means no limit The goal of
  // these parameters is to prevent waiting for a database connection.
  private int poolMaxTotal = DEFAULT_MAX_TOTAL;
  // Maximum number of database connections to keep idle.
  private int poolMaxIdle = DEFAULT_MAX_TOTAL;

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
        .append("jmxEnabled", jmxEnabled)
        .append("poolMaxTotal", poolMaxTotal)
        .append("poolMaxIdle", poolMaxIdle)
        .toString();
  }
}
