package bio.terra.common.prometheus;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {PrometheusProperties.class})
public class PrometheusConfig {
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(
      name = "terra.common.prometheus.endpointEnabled",
      havingValue = "true",
      matchIfMissing = true)
  public PrometheusHttpServer prometheusHttpServer(PrometheusProperties prometheusProperties) {
    return PrometheusHttpServer.builder().setPort(prometheusProperties.getEndpointPort()).build();
  }
}
