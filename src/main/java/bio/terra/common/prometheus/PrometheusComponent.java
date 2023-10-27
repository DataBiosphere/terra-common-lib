package bio.terra.common.prometheus;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.resources.Resource;
import io.prometheus.client.exporter.HTTPServer;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;

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
  public SdkMeterProvider prometheusMeterProvider(
      Resource otelResource,
      Optional<PrometheusHttpServer> prometheusHttpServer,
      ObjectProvider<Pair<InstrumentSelector, View>> views) {
    var sdkMeterProviderBuilder = SdkMeterProvider.builder().addResource(otelResource);
    prometheusHttpServer.ifPresent(sdkMeterProviderBuilder::registerMetricReader);
    views.stream()
        .forEach(pair -> sdkMeterProviderBuilder.registerView(pair.getFirst(), pair.getSecond()));
    return sdkMeterProviderBuilder.build();
  }
}
