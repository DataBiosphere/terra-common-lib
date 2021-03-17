package bio.terra.common.stairway;

import static com.google.cloud.ServiceOptions.getDefaultProjectId;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.exception.StairwayExecutionException;
import java.util.Optional;
import javax.inject.Provider;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {StairwayProperties.class, KubeProperties.class})
public class StairwayConfig {

  private final StairwayProperties stairwayProperties;
  private final KubeProperties kubeProperties;

  public StairwayConfig(StairwayProperties stairwayProperties, KubeProperties kubeProperties) {
    this.stairwayProperties = stairwayProperties;
    this.kubeProperties = kubeProperties;
  }

  @Bean
  @Qualifier("stairway.cluster.name")
  public String getStairwayClusterName(
      KubeService kubeService, KubeProperties kubernetesProperties) {
    return kubeService.getNamespace()
        + Optional.ofNullable(kubernetesProperties.getNamespaceSuffix()).orElse(StringUtils.EMPTY)
        + "--stairwaycluster";
  }

  @Bean
  public Stairway getStairway(
      ApplicationContext applicationContext,
      @Qualifier("stairway.cluster.name") Provider<String> stairwayClusterNameProvider) {
    Stairway.Builder builder =
        Stairway.newBuilder()
            .maxParallelFlights(stairwayProperties.getMaxParallelFlights())
            .applicationContext(applicationContext)
            .keepFlightLog(true)
            .stairwayName(kubeProperties.getPodName())
            .stairwayClusterName(stairwayClusterNameProvider.get())
            .workQueueProjectId(getDefaultProjectId())
            .enableWorkQueue(kubeProperties.isInKubernetes())
            .stairwayHook(new TracingHook());
    try {
      return builder.build();
    } catch (StairwayExecutionException e) {
      throw new IllegalArgumentException("Failed to build Stairway.", e);
    }
  }
}
