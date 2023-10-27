package bio.terra.common.opentelemetry;

import io.opentelemetry.exporter.prometheus.PrometheusHttpServer;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;

@Configuration
@EnableOpenTelemetry
public class OpenTelemetryConfig {
  @Bean
  @Primary
  public SdkMeterProvider terraMeterProvider(
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
