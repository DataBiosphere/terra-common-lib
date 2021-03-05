package bio.terra.common.kubernetes;

import bio.terra.stairway.Stairway;
import bio.terra.stairway.exception.DatabaseOperationException;
import bio.terra.stairway.exception.StairwayExecutionException;
import com.google.common.reflect.TypeToken;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Watch;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Kubernetes Pod Listener listens for pod creation and deletion events within a namespace. When
 * a pod is deleted, the listener tells Stairway to perform recovery on flights owned by the
 * Stairway with that pod name. It works so long as the pod name is used as the stairway instance
 * name when the Stairway object is created.
 *
 * <p>Typically, there are different types of pods in a namespace. We only want to listen to pods
 * that are making a Stairway cluster. We rely on consistent pod naming to filter for those pods.
 * The listener accepts a string and only attends to pods whose names contain that string.
 *
 * <p>Experience shows that the watch on the Kubernetes pod list is not stable. It drops and is
 * recreated in a retry loop. Happily, recreating the watch causes Kubernetes to reiterate all of
 * the current pods, so the listener does not miss state changes.
 */
@SuppressFBWarnings(
    value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
    justification = "Spotbugs doesn't understand resource try construct")
class KubePodListener implements Runnable {

  enum PodState {
    RUNNING,
    DELETED
  }

  private static final int WATCH_RETRIES = 10;
  private static final int WATCH_INITIAL_WAIT = 5;
  private static final int WATCH_MAX_WAIT = 30;

  private final Logger logger = LoggerFactory.getLogger(KubePodListener.class);
  private final KubeService kubeService;
  private final String namespace;
  private final String podNameFilter;
  private final Stairway stairway;
  private Exception exception;
  private final Map<String, PodState> podMap;

  /**
   * Setup the listener configuration.
   *
   * @param kubeService The parent service, used to check the shutdown state
   * @param stairway The Stairway instance to use for recovering deleted pods
   * @param namespace Kubernetes namespace to listen in
   * @param podNameFilter Only pods with names containing this string are attended to by the
   *     listener
   */
  KubePodListener(
      KubeService kubeService, Stairway stairway, String namespace, String podNameFilter) {
    this.kubeService = kubeService;
    this.namespace = namespace;
    this.podNameFilter = podNameFilter;
    this.stairway = stairway;
    this.exception = null;

    // The map must be concurrent, since the computation of active pods may be done on different
    // threads than the pod listener accesses.
    podMap = new ConcurrentHashMap<>();
  }

  @Override
  public void run() {
    logger.info("KubePodListener starting");

    exception = null;
    int consecutiveRetryCount = 0;
    int retryWait = WATCH_INITIAL_WAIT;

    // retry loop
    while (true) {
      try {
        final ApiClient defaultClient = Config.defaultClient();
        // infinite timeout
        final OkHttpClient httpClient =
            defaultClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
        defaultClient.setHttpClient(httpClient);
        Configuration.setDefaultApiClient(defaultClient);
        final CoreV1Api kubeApi = new CoreV1Api();

        // Instantiate the watch and scope it.
        try (Watch<V1Namespace> watch = makeWatch(defaultClient, kubeApi)) {
          for (Watch.Response<V1Namespace> item : watch) {
            // If we are shutting down, we stop watching
            if (kubeService.isShutdown()) {
              return;
            }
            // Reset retry if the watch worked
            consecutiveRetryCount = 0;
            retryWait = WATCH_INITIAL_WAIT;

            final String operation = item.type;
            final String podName =
                Optional.ofNullable(item.object.getMetadata())
                    .map(V1ObjectMeta::getName)
                    .orElse("");
            logger.info(String.format("%s : %s", operation, podName));

            if (handlePodOperation(operation, podName)) {
              // interrupted
              return;
            }
          }
        }
      } catch (RuntimeException | ApiException | IOException ex) {
        exception = ex;
        logger.info("KubePodListener caught exception: " + exception);
      }

      // Exponential backoff retry after an exception from the watch
      // Now and then, the watch gets a network timeout. This loop restarts it. We don't want a
      // hard failure to just continue to loop, so if it hits the error WATCH_RETRIES times in a
      // row, we bail out of the listener.
      if (consecutiveRetryCount >= WATCH_RETRIES) {
        logger.error("KubePodListener exiting - exceeded max consecutive retries", exception);
        return;
      }
      consecutiveRetryCount++;

      try {
        logger.info("KubePodListener retry wait seconds: " + retryWait);
        TimeUnit.SECONDS.sleep(retryWait);
        retryWait = Math.min(retryWait + retryWait, WATCH_MAX_WAIT);
      } catch (InterruptedException ex) {
        logger.info("KubePodListener exiting - interruped while retrying");
        return;
      }
      logger.info("KubePodListener consecutive retry: " + consecutiveRetryCount);
    }
  }

  private Watch<V1Namespace> makeWatch(ApiClient apiClient, CoreV1Api kubeApi) throws ApiException {
    return Watch.createWatch(
        apiClient,
        kubeApi.listNamespacedPodCall(
            namespace, null, null, null, null, null, 5, null, null, Boolean.TRUE, null),
        new TypeToken<Watch.Response<V1Namespace>>() {}.getType());
  }

  /**
   * Process a single pod/operation pair.
   *
   * @param operation - pod operation, such as ADDED or DELETED
   * @param podName - name of pod to map or recover
   * @return true if stairway recovery was interrupted, false otherwise.
   */
  private boolean handlePodOperation(String operation, String podName) {
    if (StringUtils.contains(podName, podNameFilter)) {
      if ("ADDED".equals(operation)) {
        logger.info("Added api pod: " + podName);
        podMap.put(podName, PodState.RUNNING);
      } else if ("DELETED".equals(operation)) {
        try {
          recoverDeletedPod(podName);
        } catch (DatabaseOperationException | StairwayExecutionException ex) {
          logger.error("Stairway recoverStairway failed to recover pod: " + podName, ex);
        } catch (InterruptedException ex) {
          logger.info("KubePodListener interrupted - exiting", ex);
          exception = ex;
          return true;
        }
      }
    }
    return false;
  }

  // Attempt to recover a deleted pod using Stairway. Update the map to indicate the pod is deleted.
  private void recoverDeletedPod(String podName)
      throws DatabaseOperationException, InterruptedException, StairwayExecutionException {
    logger.info("Attempting clean up of deleted stairway instance: " + podName);
    stairway.recoverStairway(podName);
    markDeletedIfRunning(podName);
  }

  private void markDeletedIfRunning(String podName) {
    PodState deletedPodValue = podMap.get(podName);
    if (deletedPodValue == PodState.RUNNING) {
      logger.info("Deleted api pod: " + podName);
      podMap.put(podName, PodState.DELETED);
    }
  }

  Exception getException() {
    return exception;
  }

  Map<String, PodState> getPodMap() {
    return podMap;
  }

  // get the number of running pods from the podMap
  int getActivePodCount() {
    final long count = podMap.values().stream().filter(PodState.RUNNING::equals).count();
    logger.info("KubePodListener ActivePodCount: {} of {} pods active", count, podMap.size());
    return (int) count;
  }
}
