package bio.terra.common.db;

import org.apache.commons.dbcp2.*;

/** Base class for accessing JDBC configuration properties. */
public class DatabaseProperties {
  private boolean jmxEnabled = true;
  private String uri;
  private String username;
  private String password;

  // If true, the database will be wiped when start
  private boolean initializeOnStart;
  // If true, the database will have changesets applied when start
  private boolean upgradeOnStart;

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

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  // NOTE: even though the setters appear unused, the Spring infrastructure uses them to populate
  // the properties.
  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setUsername(String username) {
    this.username = username;
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
}
