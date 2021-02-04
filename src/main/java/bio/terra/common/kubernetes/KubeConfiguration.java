package bio.terra.common.kubernetes;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "terra.common")
public class KubeConfiguration {

  /**
   * Name of the Kubernetes pod we are running in. If we are not in a pod, this defaults to a
   * constant string in application properties.
   */
  private String podName;

  /**
   * Used to denote that we are running in the Kubernetes environment. This should NOT be changed in
   * application.properties. It should only be reset by the Kubernetes deployment.
   */
  private boolean inKubernetes;

  /** Filter to apply to pod names returned by Kubernetes API. */
  private String apiPodFilter;

  /** File containing Kubernetes namespace to use. */
  private String namespaceFile;

  public String getPodName() {
    return podName;
  }

  public void setPodName(String podName) {
    this.podName = podName;
  }

  public boolean isInKubernetes() {
    return inKubernetes;
  }

  public void setInKubernetes(boolean inKubernetes) {
    this.inKubernetes = inKubernetes;
  }

  public String getApiPodFilter() {
    return apiPodFilter;
  }

  public void setPApiPodFilter(String apiPodFilter) {
    this.apiPodFilter = apiPodFilter;
  }

  public String getNamespaceFile() {
    return namespaceFile;
  }

  public void setNamespaceFile(String namespaceFile) {
    this.namespaceFile = namespaceFile;
  }
}
