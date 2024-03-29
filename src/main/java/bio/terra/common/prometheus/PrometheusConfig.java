package bio.terra.common.prometheus;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "bio.terra.common.opentelemetry")
@EnableConfigurationProperties(value = {PrometheusProperties.class})
public class PrometheusConfig {
  private final Logger logger = LoggerFactory.getLogger(PrometheusConfig.class);

  /** Creates OpenTelemetry MetricReader that exports metrics to Prometheus HTTP server */
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(
      name = "terra.common.prometheus.endpointEnabled",
      havingValue = "true",
      matchIfMissing = true)
  public MetricReader prometheusHttpServer(PrometheusProperties prometheusProperties) {
    logger.info("Prometheus metrics enabled.");
    return PrometheusHttpServer.builder().setPort(prometheusProperties.getEndpointPort()).build();
  }
}
