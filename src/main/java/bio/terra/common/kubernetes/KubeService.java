package bio.terra.common.kubernetes;

import bio.terra.common.kubernetes.exception.KubeApiException;
import bio.terra.stairway.Stairway;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * KubeService provides access to a given Kubernetes environment.
 *
 * <p>The most common use in McTerra is for services that use Stairway to be able to recover flights
 * that fail when a pod goes away. The current convention is to use the pod name as the stairway
 * name. Then the service can compare the list of stairways it thinks should be running, compare
 * with list of active pods, and recover anything that is no longer active.
 */
@Component
public class KubeService {
  private static final Logger logger = LoggerFactory.getLogger(KubeService.class);

  private final KubeConfiguration config;
  private final KubeShutdownState shutdownState;
  private final String podName;
  private final String namespace;
  private final boolean inKubernetes;
  private final String apiPodFilter;

  private KubePodListener podListener;
  private Thread podListenerThread;

  @Autowired
  public KubeService(KubeConfiguration config, KubeShutdownState shutdownState) {
    this.config = config;
    this.shutdownState = shutdownState;
    this.podName = config.getPodName();
    this.inKubernetes = config.isInKubernetes();
    this.apiPodFilter = config.getApiPodFilter();
    if (inKubernetes) {
      this.namespace = readFileIntoString(config.getNamespaceFile());
    } else {
      this.namespace = "nonamespace";
    }

    logger.info(
        "Kubernetes configuration: inKube: "
            + inKubernetes
            + "; namespace: "
            + namespace
            + "; podName: "
            + podName);
  }

  /**
   * Get a list of the API pods from Kubernetes. It is returned as a set to make probing for
   * specific names easy.
   *
   * @return set of pod names containing the API_POD_FILTER string; null if not in kubernetes
   */
  public Set<String> getApiPodList() {
    Set<String> pods = new HashSet<>();
    if (!inKubernetes) {
      return pods;
    }

    try {
      CoreV1Api api = makeCoreApi();
      V1PodList list =
          api.listNamespacedPod(namespace, null, null, null, null, null, null, null, null, null);
      for (V1Pod item : list.getItems()) {
        String podName = item.getMetadata().getName();
        if (StringUtils.contains(podName, apiPodFilter)) {
          pods.add(podName);
        }
      }
      return pods;
    } catch (ApiException ex) {
      throw new KubeApiException("Error listing pods", ex);
    }
  }

  public String getApiDeploymentUid() {
    // We want deployment to be unique for every run in the non-Kubernetes environment
    String uid = "fake" + UUID.randomUUID().toString();
    if (inKubernetes) {
      V1Deployment deployment = getApiDeployment();
      if (deployment != null) {
        uid = deployment.getMetadata().getUid();
      }
    }
    return uid;
  }

  // Common method to pull out the api deployment. We expect to have only one api deployment. If
  // there is more than
  // one, this will return the first one it finds.
  private V1Deployment getApiDeployment() {
    try {
      AppsV1Api appsapi = makeDeploymentApi();
      V1DeploymentList deployments =
          appsapi.listNamespacedDeployment(
              namespace, null, null, null, null, null, null, null, null, null);

      for (V1Deployment item : deployments.getItems()) {
        String deploymentName = item.getMetadata().getName();
        if (StringUtils.contains(deploymentName, apiPodFilter)) {
          return item;
        }
      }
    } catch (ApiException ex) {
      throw new KubeApiException("Error listing deployments", ex);
    }
    return null;
  }

  /**
   * Launch the {@link KubePodListener} thread.
   *
   * @param stairway the Stairway instance passed to the pod listener for Stairway recovery
   */
  public void startPodListener(Stairway stairway) {
    if (inKubernetes) {
      podListener = new KubePodListener(shutdownState, stairway, namespace, apiPodFilter);
      podListenerThread = new Thread(podListener);
      podListenerThread.start();
    }
  }

  /**
   * Stop the pod listener thread within a given time span.
   *
   * @param timeUnit unit of the joinWait
   * @param joinWait number of units to wait for the listener thread to stop
   * @return true if the thread joined in the time span. False otherwise.
   */
  public boolean stopPodListener(TimeUnit timeUnit, long joinWait) {
    if (inKubernetes) {
      podListenerThread.interrupt();
      long waitMillis = timeUnit.toMillis(joinWait);
      try {
        podListenerThread.join(waitMillis);
      } catch (InterruptedException ex) {
        return false;
      }
    }
    return true;
  }

  public int getActivePodCount() {
    if (podListener != null) {
      return podListener.getActivePodCount();
    }
    int defaultPodCount = 1;
    logger.info("KubeService ActivePodCount - default val: {}", defaultPodCount);
    return defaultPodCount;
  }

  private CoreV1Api makeCoreApi() {
    try {
      ApiClient client = ClientBuilder.cluster().build();
      Configuration.setDefaultApiClient(client);
      return new CoreV1Api();
    } catch (IOException ex) {
      throw new KubeApiException("Error making core API", ex);
    }
  }

  private AppsV1Api makeDeploymentApi() {
    try {
      ApiClient client = ClientBuilder.cluster().build();
      Configuration.setDefaultApiClient(client);
      return new AppsV1Api();
    } catch (IOException ex) {
      throw new KubeApiException("Error making deployment API", ex);
    }
  }

  public String getPodName() {
    return podName;
  }

  public String getNamespace() {
    return namespace;
  }

  private String readFileIntoString(String path) {
    try {
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      return new String(encoded, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Failed to read file: " + path + "; ", e);
      return null;
    }
  }
}
