package bio.terra.common.kubernetes;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@EnableConfigurationProperties(KubeProperties.class)
public class KubeConfig {

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public KubeService getKubeService(KubeProperties kubernetesProperties) {
    return new KubeService(
        kubernetesProperties.getPodName(),
        kubernetesProperties.isInKubernetes(),
        kubernetesProperties.getPodNameFilter());
  }
}
