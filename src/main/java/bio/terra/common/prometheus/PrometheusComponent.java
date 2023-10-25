package bio.terra.common.prometheus;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.prometheus.client.exporter.HTTPServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Spring Beans that exposes any OpenTelemetry metrics via a Prometheus {@link HTTPServer}. */
@Configuration
@EnableOpenTelemetry
public class PrometheusComponent {
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(
      name = "terra.common.prometheus.endpointEnabled",
      havingValue = "true",
      matchIfMissing = true)
  public PrometheusHttpServer prometheusHttpServer(PrometheusProperties prometheusProperties) {
    return PrometheusHttpServer.builder().setPort(prometheusProperties.getEndpointPort()).build();
  }

  @Bean
  @Primary
  @ConditionalOnBean(PrometheusHttpServer.class)
  public SdkMeterProvider prometheusMeterProvider(
      Resource otelResource, PrometheusHttpServer prometheusHttpServer) {
    return SdkMeterProvider.builder()
        .addResource(otelResource)
        .registerMetricReader(prometheusHttpServer)
        .build();
  }
}
