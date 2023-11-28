package bio.terra.common.stairway;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.stairway.*;
import bio.terra.stairway.azure.AzureServiceBusQueue;
import bio.terra.stairway.exception.StairwayException;
import bio.terra.stairway.exception.StairwayExecutionException;
import bio.terra.stairway.gcp.GcpPubSubQueue;
import bio.terra.stairway.gcp.GcpQueueUtils;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.cloud.ServiceOptions.getDefaultProjectId;

/** A Spring Component for exposing an initialized {@link Stairway}. */
@Component
public class StairwayComponent {
  private final Logger logger = LoggerFactory.getLogger(StairwayComponent.class);

  private final KubeService kubeService;
  private final KubeProperties kubeProperties;
  private final StairwayProperties stairwayProperties;
  private final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZING);
  private Stairway stairway;

  @Autowired
  public StairwayComponent(
      KubeService kubeService,
      KubeProperties kubeProperties,
      StairwayProperties stairwayProperties) {
    this.kubeService = kubeService;
    this.kubeProperties = kubeProperties;
    this.stairwayProperties = stairwayProperties;
    logger.info("Creating Stairway: name: [{}]", kubeService.getPodName());
  }

  /**
   * Set up the Stairway work queue. There are two ways we get a work queue. If both the gcp topicId
   * and subscriptionId are passed as parameters, then we use those. Otherwise, if the
   * clusterNameSuffix is present, then we create the queue. It is an error to try to run in
   * Kubernetes without a work queue.
   */
  private QueueInterface setupGcpWorkQueue() {
    try {
      String topicId = stairwayProperties.getGcpPubSubTopicId();
      String subscriptionId = stairwayProperties.getGcpPubSubSubscriptionId();
      String clusterNameSuffix = stairwayProperties.getClusterNameSuffix();

      if (topicId == null && subscriptionId == null && clusterNameSuffix != null) {
        // work queue needs to be created
        String clusterName =
            String.format(
                "%s-%s", kubeService.getNamespace(), stairwayProperties.getClusterNameSuffix());
        topicId = clusterName + "-workqueue";
        subscriptionId = clusterName + "-workqueue-sub";
        GcpQueueUtils.makeTopic(getDefaultProjectId(), topicId);
        GcpQueueUtils.makeSubscription(getDefaultProjectId(), topicId, subscriptionId);
        logger.info(
            "Found or created work queue. Topic: {}; Subscription: {}", topicId, subscriptionId);
      } else {
        // If the work queue is not being properly passed in
        if (!(topicId != null && subscriptionId != null && clusterNameSuffix == null)) {
          throw new IllegalArgumentException(
              "Invalid stairway configuration. You must either specify the clusterNameSuffix"
                  + " or both the topicId and subscriptionId.");
        }
      }

      return GcpPubSubQueue.newBuilder()
          .projectId(getDefaultProjectId())
          .topicId(topicId)
          .subscriptionId(subscriptionId)
          .build();

    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to configure pubsub queue", e);
    }
  }


  /**
   * Set up the Stairway Azure work queue.
   */
  @VisibleForTesting
  QueueInterface setupAzureWorkQueue() {
    return AzureServiceBusQueue.newBuilder()
              .connectionString(stairwayProperties.getAzureServiceBusConnectionString())
              .subscriptionName(stairwayProperties.getAzureServiceBusSubscriptionName())
              .topicName(stairwayProperties.getAzureServiceBusTopicName())
              .maxAutoLockRenewDuration(Duration.ofMinutes(stairwayProperties.getAzureServiceBusMaxAutoLockRenewDuration()))
              .namespace(stairwayProperties.getAzureServiceBusNamespace())
              .useManagedIdentity(stairwayProperties.isUseManagedIdentity())
              .build();
  }

  /** convenience for getting a builder for initialize input */
  public StairwayOptionsBuilder newStairwayOptionsBuilder() {
    return new StairwayOptionsBuilder();
  }

  /**
   * @deprecated Build and initialize the Stairway object Deprecated. Use the
   * @param dataSource data source for the Stairway DB
   * @param context application context or other service-specific contextual object
   * @param hooks list of Stairway hooks to install when building Stairway
   */
  @Deprecated
  public void initialize(DataSource dataSource, Object context, List<StairwayHook> hooks) {
    initialize(newStairwayOptionsBuilder().dataSource(dataSource).context(context).hooks(hooks));
  }

  /**
   * Build and initialize the Stairway object - extensible version
   *
   * @param initializeBuilder collection of Stairway initialization parameters
   */
  public void initialize(StairwayOptionsBuilder initializeBuilder) {
    QueueInterface queue;
    //Using Azure WorkQueue if azureQueueEnabled set to true
    if(stairwayProperties.isAzureQueueEnabled()) {
      queue = setupAzureWorkQueue();
    } else {
      queue = (kubeProperties.isInKubernetes()) ? setupGcpWorkQueue() : null;
    }

    logger.info("Initializing Stairway...");
    final StairwayBuilder builder =
        new StairwayBuilder()
            .maxParallelFlights(stairwayProperties.getMaxParallelFlights())
            .retentionCheckInterval(stairwayProperties.getRetentionCheckInterval())
            .completedFlightRetention(stairwayProperties.getCompletedFlightRetention())
            .applicationContext(
                initializeBuilder.getContext()) // not necessarily a Spring ApplicationContext
            .stairwayName(kubeProperties.getPodName())
            .workQueue(queue)
            .exceptionSerializer(initializeBuilder.getExceptionSerializer());
    initializeBuilder.getHooks().forEach(builder::stairwayHook);
    try {
      this.stairway = builder.build();
    } catch (StairwayExecutionException e) {
      throw new RuntimeException("Failed to build Stairway.", e);
    }

    try {
      List<String> recordedStairways =
          stairway.initialize(
              initializeBuilder.getDataSource(),
              stairwayProperties.isForceCleanStart(),
              stairwayProperties.isMigrateUpgrade());

      kubeService.startPodListener(stairway);

      // Lookup all of the stairway instances we know about
      Set<String> existingStairways = kubeService.getPodSet();
      List<String> obsoleteStairways = new LinkedList<>();

      // Any instances that stairway knows about, but we cannot see are obsolete.
      for (String recordedStairway : recordedStairways) {
        if (!existingStairways.contains(recordedStairway)) {
          obsoleteStairways.add(recordedStairway);
        }
      }

      // Add our own pod name to the list of obsolete stairways. Sometimes Kubernetes will
      // restart the container without redeploying the pod. In that case we must ask
      // Stairway to recover the flights we were working on before being restarted.
      obsoleteStairways.add(kubeService.getPodName());

      logger.info(
          "existingStairways: {}. obsoleteStairways: {}", existingStairways, obsoleteStairways);
      // Recover and start stairway - step 3 of the stairway startup sequence
      stairway.recoverAndStart(obsoleteStairways);
    } catch (StairwayException | InterruptedException e) {
      status.compareAndSet(Status.INITIALIZING, Status.ERROR);
      throw new RuntimeException("Error starting Stairway", e);
    }
    status.compareAndSet(Status.INITIALIZING, Status.OK);
  }

  /** Stop accepting jobs and shutdown stairway. Returns true if successful. */
  public boolean shutdown() throws InterruptedException {
    status.set(Status.SHUTDOWN);
    logger.info("Request Stairway shutdown");
    boolean shutdownSuccess =
        stairway.quietDown(
            stairwayProperties.getQuietDownTimeout().toMillis(), TimeUnit.MILLISECONDS);
    if (!shutdownSuccess) {
      logger.info("Request Stairway terminate");
      shutdownSuccess =
          stairway.terminate(
              stairwayProperties.getTerminateTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    logger.info("Finished Stairway shutdown?: {}", shutdownSuccess);
    return shutdownSuccess;
  }

  public Stairway get() {
    return stairway;
  }

  public StairwayComponent.Status getStatus() {
    return status.get();
  }

  public enum Status {
    INITIALIZING,
    OK,
    ERROR,
    SHUTDOWN
  }

  /**
   * Builder to supply Stairway settings to the {@link #initialize(StairwayOptionsBuilder)} method.
   * Using a builder model allows us to add parameters without adding new methods to the Stairway
   * component.
   */
  public static class StairwayOptionsBuilder {
    private DataSource dataSource;
    private Object context;
    private List<StairwayHook> hooks = new ArrayList<>();
    private ExceptionSerializer exceptionSerializer;

    public DataSource getDataSource() {
      return dataSource;
    }

    public StairwayOptionsBuilder dataSource(DataSource dataSource) {
      this.dataSource = dataSource;
      return this;
    }

    public Object getContext() {
      return context;
    }

    public StairwayOptionsBuilder context(Object context) {
      this.context = context;
      return this;
    }

    public List<StairwayHook> getHooks() {
      return hooks;
    }

    public StairwayOptionsBuilder hooks(List<StairwayHook> hooks) {
      this.hooks = hooks;
      return this;
    }

    public ExceptionSerializer getExceptionSerializer() {
      return exceptionSerializer;
    }

    public StairwayOptionsBuilder exceptionSerializer(ExceptionSerializer exceptionSerializer) {
      this.exceptionSerializer = exceptionSerializer;
      return this;
    }

    public StairwayOptionsBuilder addHook(StairwayHook hook) {
      hooks.add(hook);
      return this;
    }
  }
}
