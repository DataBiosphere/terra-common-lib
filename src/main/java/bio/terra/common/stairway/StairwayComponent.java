package bio.terra.common.stairway;

import static com.google.cloud.ServiceOptions.getDefaultProjectId;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.StairwayHook;
import bio.terra.stairway.exception.StairwayException;
import bio.terra.stairway.exception.StairwayExecutionException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A Spring Component for exposing an initialized {@link Stairway}. */
@Component
public class StairwayComponent {
  private final Logger logger = LoggerFactory.getLogger(StairwayComponent.class);

  private final KubeService kubeService;
  private final KubeProperties kubeProperties;
  private Stairway stairway;
  private final StairwayProperties stairwayProperties;

  public enum Status {
    INITIALIZING,
    OK,
    ERROR,
    SHUTDOWN,
  }

  private final AtomicReference<Status> status = new AtomicReference<>(Status.INITIALIZING);

  @Autowired
  public StairwayComponent(
      KubeService kubeService,
      KubeProperties kubeProperties,
      StairwayProperties stairwayProperties) {
    this.kubeService = kubeService;
    this.kubeProperties = kubeProperties;
    this.stairwayProperties = stairwayProperties;
    logger.info(
        "Creating Stairway: name: [{}]  cluster name: [{}]",
        kubeService.getPodName(),
        getClusterName());
  }

  /**
   * Build and initialize the Stairway object
   *
   * @param dataSource data source for the Stairway DB
   * @param context application context or other service-specific contextual object
   * @param hooks list of Stairway hooks to install when building Stairway
   */
  public void initialize(DataSource dataSource, Object context, List<StairwayHook> hooks) {
    logger.info("Initializing Stairway...");
    final Stairway.Builder builder =
        Stairway.newBuilder()
            .maxParallelFlights(stairwayProperties.getMaxParallelFlights())
            .applicationContext(context) // not necessarily a Spring ApplicationContext
            .keepFlightLog(true)
            .stairwayName(kubeProperties.getPodName())
            .stairwayClusterName(getClusterName())
            .workQueueProjectId(getDefaultProjectId())
            .enableWorkQueue(kubeProperties.isInKubernetes());
    hooks.forEach(builder::stairwayHook);
    try {
      this.stairway = builder.build();
    } catch (StairwayExecutionException e) {
      throw new RuntimeException("Failed to build Stairway.", e);
    }

    try {
      List<String> recordedStairways =
          stairway.initialize(
              dataSource,
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

  private String getClusterName() {
    return String.format("%s-%s", kubeService.getNamespace(), stairwayProperties.getClusterNameSuffix());
  }
}
