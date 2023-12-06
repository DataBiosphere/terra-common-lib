package bio.terra.common.stairway;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for configuring a Stairway instance.
 *
 * <p>There are three parameters involved in configuring the Stairway work queue. There are three
 * valid setups:
 *
 * <ol>
 *   <li>If we are not running in Kubernetes, these parameters are ignored
 *   <li>clusterNameSuffix is null. gcpPubSubTopicId, and gcpPubSubSubscriptionId are provided: the
 *       topic and subscrition are used for the work queue.
 *   <li>clusterNameSuffix is provided. gcpPubSubTopicId and gcpPubSubSubscriptionId are null:
 *       topicId and subscriptionId are generated based on the clusterNameSuffix and the pubsub
 *       topic and subscription are created or found to already exist.
 * </ol>
 */
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

  /**
   * clusterNameSuffix is used to generate names when creating pubsub queues. It must be null if the
   * topicId and subscriptionId are provided.
   */
  private String clusterNameSuffix;

  /**
   * PubSub topic to use for stairway work queue. It must exist in the current GCP project. It must
   * be null if clusterNameSuffix is provided.
   */
  private String gcpPubSubTopicId;

  /**
   * PubSub subscription to use for stairway work queue. It must exist in the current GCP project.
   * It must be null if clusterNameSuffix is provided.
   */
  private String gcpPubSubSubscriptionId;

  /**
   * azureServiceBusMaxAutoLockRenewDuration represents the maximum duration for which the lock on a
   * message will be automatically renewed. This helps in extending the lock duration to ensure that
   * the message is not released to other competing consumers while it is being processed.
   */
  private long azureServiceBusMaxAutoLockRenewDuration;

  /**
   * azureServiceBusConnectionString is a string that contains the information required to connect
   * to a Service Bus namespace. It typically includes details such as the endpoint URL, the shared
   * access key name, and the shared access key.
   */
  private String azureServiceBusConnectionString;

  /**
   * useManagedIdentity with Azure Service Bus allows applications running on Azure services, such
   * as Azure Virtual Machines or Azure App Service, to authenticate with Service Bus without the
   * need for explicit credential management. Managed Identity eliminates the need to store
   * credentials in code or configuration files.
   */
  private boolean useManagedIdentity;

  /**
   * azureServiceBusNamespace is a container for messaging entities, including queues, topics, and
   * subscriptions.
   */
  private String azureServiceBusNamespace;

  /** Azure Service Bus TopicName */
  private String azureServiceBusTopicName;

  /** Azure Service Bus subscription name */
  private String azureServiceBusSubscriptionName;

  /**
   * azureQueueEnabled need to set to true to use Azure Service Bus as a Work Queue
   * azureQueueEnabled = true Azure Service Bus will be the work queue, azureQueueEnabled = false
   * Gcp PubSub will be the work queue
   */
  private boolean azureQueueEnabled;

  public boolean isForceCleanStart() {
    return forceCleanStart;
  }

  public void setForceCleanStart(boolean forceCleanStart) {
    this.forceCleanStart = forceCleanStart;
  }

  public boolean isMigrateUpgrade() {
    return migrateUpgrade;
  }

  public void setMigrateUpgrade(boolean migrateUpgrade) {
    this.migrateUpgrade = migrateUpgrade;
  }

  public int getMaxParallelFlights() {
    return maxParallelFlights;
  }

  public void setMaxParallelFlights(int maxParallelFlights) {
    this.maxParallelFlights = maxParallelFlights;
  }

  public Duration getQuietDownTimeout() {
    return quietDownTimeout;
  }

  public void setQuietDownTimeout(Duration quietDownTimeout) {
    this.quietDownTimeout = quietDownTimeout;
  }

  public Duration getTerminateTimeout() {
    return terminateTimeout;
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

  public String getGcpPubSubTopicId() {
    return gcpPubSubTopicId;
  }

  public void setGcpPubSubTopicId(String gcpPubSubTopicId) {
    this.gcpPubSubTopicId = gcpPubSubTopicId;
  }

  public String getGcpPubSubSubscriptionId() {
    return gcpPubSubSubscriptionId;
  }

  public void setGcpPubSubSubscriptionId(String gcpPubSubSubscriptionId) {
    this.gcpPubSubSubscriptionId = gcpPubSubSubscriptionId;
  }

  public long getAzureServiceBusMaxAutoLockRenewDuration() {
    return azureServiceBusMaxAutoLockRenewDuration;
  }

  public void setAzureServiceBusMaxAutoLockRenewDuration(
      long azureServiceBusMaxAutoLockRenewDuration) {
    this.azureServiceBusMaxAutoLockRenewDuration = azureServiceBusMaxAutoLockRenewDuration;
  }

  public String getAzureServiceBusConnectionString() {
    return azureServiceBusConnectionString;
  }

  public void setAzureServiceBusConnectionString(String azureServiceBusConnectionString) {
    this.azureServiceBusConnectionString = azureServiceBusConnectionString;
  }

  public boolean isUseManagedIdentity() {
    return useManagedIdentity;
  }

  public void setUseManagedIdentity(boolean useManagedIdentity) {
    this.useManagedIdentity = useManagedIdentity;
  }

  public String getAzureServiceBusNamespace() {
    return azureServiceBusNamespace;
  }

  public void setAzureServiceBusNamespace(String azureServiceBusNamespace) {
    this.azureServiceBusNamespace = azureServiceBusNamespace;
  }

  public String getAzureServiceBusTopicName() {
    return azureServiceBusTopicName;
  }

  public void setAzureServiceBusTopicName(String azureServiceBusTopicName) {
    this.azureServiceBusTopicName = azureServiceBusTopicName;
  }

  public String getAzureServiceBusSubscriptionName() {
    return azureServiceBusSubscriptionName;
  }

  public void setAzureServiceBusSubscriptionName(String azureServiceBusSubscriptionName) {
    this.azureServiceBusSubscriptionName = azureServiceBusSubscriptionName;
  }

  public boolean isAzureQueueEnabled() {
    return azureQueueEnabled;
  }

  public void setAzureQueueEnabled(boolean azureQueueEnabled) {
    this.azureQueueEnabled = azureQueueEnabled;
  }
}
