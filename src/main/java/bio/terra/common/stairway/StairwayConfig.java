package bio.terra.common.stairway;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {StairwayProperties.class})
public class StairwayConfig {

  //  private final StairwayProperties stairwayProperties;
  //  private final KubeProperties kubeProperties;
  //
  //  public StairwayConfig(StairwayProperties stairwayProperties, KubeProperties kubeProperties) {
  //    this.stairwayProperties = stairwayProperties;
  //    this.kubeProperties = kubeProperties;
  //  }

  // TODO: make the cluster name a property
  @Bean(name = "stairway.cluster.name")
  public String getStairwayClusterName(KubeService kubeService, KubeProperties kubeProperties) {
    return kubeService.getNamespace()
        + Optional.ofNullable(kubeProperties.getNamespaceSuffix()).orElse(StringUtils.EMPTY)
        + "--stairwaycluster";
  }

  //  @Bean
  //  public Stairway getStairway(
  //      Object context,
  //      @Qualifier("stairway.cluster.name") Provider<String> stairwayClusterNameProvider) {
  //    Stairway.Builder builder =
  //        Stairway.newBuilder()
  //            .maxParallelFlights(stairwayProperties.getMaxParallelFlights())
  //            .applicationContext(context)
  //            .keepFlightLog(true)
  //            .stairwayName(kubeProperties.getPodName())
  //            .stairwayClusterName(stairwayClusterNameProvider.get())
  //            .workQueueProjectId(getDefaultProjectId())
  //            .enableWorkQueue(kubeProperties.isInKubernetes());
  //            if (stairwayProperties.isTracingEnabled()) {
  //              builder.stairwayHook(new TracingHook());
  //            }
  //            // TODO: add properties to configure other common hooks here
  //    try {
  //      return builder.build();
  //    } catch (StairwayExecutionException e) {
  //      throw new IllegalArgumentException("Failed to build Stairway.", e);
  //    }
  //  }
}
