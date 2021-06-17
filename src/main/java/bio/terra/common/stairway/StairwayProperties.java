package bio.terra.common.stairway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** This class includes the standard database properties. */
@ConfigurationProperties(prefix = "terra.common.stairway")
public class StairwayProperties {
  private boolean forceCleanStart;
  private boolean migrateUpgrade;
  private int maxParallelFlights;
  private Duration quietDownTimeout;
  private Duration terminateTimeout;
  private boolean tracingEnabled;
  private Duration retentionCheckInterval;
  private Duration completedFlightRetention;
  private String clusterNameSuffix;

  public boolean isForceCleanStart() {
    return forceCleanStart;
  }

  public boolean isMigrateUpgrade() {
    return migrateUpgrade;
  }

  public int getMaxParallelFlights() {
    return maxParallelFlights;
  }

  public Duration getQuietDownTimeout() {
    return quietDownTimeout;
  }

  public Duration getTerminateTimeout() {
    return terminateTimeout;
  }

  public void setForceCleanStart(boolean forceCleanStart) {
    this.forceCleanStart = forceCleanStart;
  }

  public void setMigrateUpgrade(boolean migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public void setMaxParallelFlights(int maxParallelFlights) {
    this.maxParallelFlights = maxParallelFlights;
  }

  public void setQuietDownTimeout(Duration quietDownTimeout) {
    this.quietDownTimeout = quietDownTimeout;
  }

  public void setTerminateTimeout(Duration terminateTimeout) {
    this.terminateTimeout = terminateTimeout;
  }

  public boolean isTracingEnabled() {
    return tracingEnabled;
  }

  public void setTracingEnabled(boolean tracingEnabled) {
    this.tracingEnabled = tracingEnabled;
  }

  public String getClusterNameSuffix() {
    return clusterNameSuffix;
  }

  public void setClusterNameSuffix(String clusterNameSuffix) {
    this.clusterNameSuffix = clusterNameSuffix;
  }

  public Duration getRetentionCheckInterval() {
    return retentionCheckInterval;
  }

  public void setRetentionCheckInterval(Duration retentionCheckInterval) {
    this.retentionCheckInterval = retentionCheckInterval;
  }

  public Duration getCompletedFlightRetention() {
    return completedFlightRetention;
  }

  public void setCompletedFlightRetention(Duration completedFlightRetention) {
    this.completedFlightRetention = completedFlightRetention;
  }
}
