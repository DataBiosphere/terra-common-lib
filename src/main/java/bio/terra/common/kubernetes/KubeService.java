package bio.terra.common.kubernetes;

import bio.terra.common.kubernetes.exception.KubeApiException;
import bio.terra.stairway.Stairway;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KubeService provides access to a given Kubernetes environment.
 *
 * <p>The most common use in McTerra is for services that use Stairway to be able to recover flights
 * that fail when a pod goes away. The current convention is to use the pod name as the stairway
 * name. Then the service can compare the list of stairways it thinks should be running, compare
 * with list of active pods, and recover anything that is no longer active.
 */
@SuppressFBWarnings(
    value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "The K8s namespace file is a valid absolute filename")
public class KubeService {
  private static final Logger logger = LoggerFactory.getLogger(KubeService.class);

  // Location in the container where Kubernetes stores service account and
  // namespace information. Kubernetes mints a service account for the container (pod?)
  // that can be used to make requests of Kubernetes.
  static final String KUBE_DIR = "/var/run/secrets/kubernetes.io/serviceaccount";
  static final String KUBE_NAMESPACE_FILE = KUBE_DIR + "/namespace";

  private final String podName;
  private final boolean inKubernetes;
  private final String podNameFilter;
  private final String namespace;
  private final AtomicBoolean isShutdown;

  private KubePodListener podListener;
  private Thread podListenerThread;

  /**
   * @param podName Name of the Kubernetes pod we are running in. If we are not in a pod, this
   *     defaults to a constant string in application properties. We cannot easily find our own pod
   *     name, so components must plumb this in using settings in the helm charts.
   * @param inKubernetes Used to denote that we are running in the Kubernetes environment. The
   *     service can be called and provides reasonable answers when inKubernetes is false.
   * @param podNameFilter Filter to apply to pod names returned by Kubernetes API to know which pods
   *     we care about.
   */
  public KubeService(String podName, boolean inKubernetes, String podNameFilter) {
    this.podName = podName;
    this.inKubernetes = inKubernetes;
    this.podNameFilter = podNameFilter;
    if (inKubernetes) {
      this.namespace = readFileIntoString(KUBE_NAMESPACE_FILE);
    } else {
      this.namespace = "nonamespace";
    }
    this.isShutdown = new AtomicBoolean(false);

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
        if (item.getMetadata() != null) {
          String podName = item.getMetadata().getName();
          if (StringUtils.contains(podName, podNameFilter)) {
            pods.add(podName);
          }
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
      V1Deployment deployment = getPodDeployment();
      if (deployment != null && deployment.getMetadata() != null) {
        uid = deployment.getMetadata().getUid();
      }
    }
    return uid;
  }

  // Method to pull out the pod deployment. We expect to have only one deployment at a time
  // for a given podNameFilter but there can be more than one in some upgrade scenarios. If
  // there is more than one, this will return the first one it finds.
  private V1Deployment getPodDeployment() {
    try {
      AppsV1Api appsapi = makeDeploymentApi();
      V1DeploymentList deployments =
          appsapi.listNamespacedDeployment(
              namespace, null, null, null, null, null, null, null, null, null);

      for (V1Deployment item : deployments.getItems()) {
        if (item.getMetadata() != null) {
          String deploymentName = item.getMetadata().getName();
          if (StringUtils.contains(deploymentName, podNameFilter)) {
            return item;
          }
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
      podListener = new KubePodListener(this, stairway, namespace, podNameFilter);
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

  public void clearShutdown() {
    isShutdown.set(false);
  }

  public void setShutdown() {
    isShutdown.set(true);
  }

  public boolean isShutdown() {
    return isShutdown.get();
  }

  public String getPodName() {
    return podName;
  }

  public String getNamespace() {
    return namespace;
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
