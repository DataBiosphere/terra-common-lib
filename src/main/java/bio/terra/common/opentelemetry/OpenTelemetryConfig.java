package bio.terra.common.opentelemetry;

import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;

@Configuration
@EnableOpenTelemetry
public class OpenTelemetryConfig {
  /**
   * Creates an OpenTelemetry {@link SdkMeterProvider} with all metrics readers and views in the
   * spring context
   */
  @Bean
  @Primary
  public SdkMeterProvider terraMeterProvider(
      Resource otelResource,
      ObjectProvider<MetricReader> metricReaders,
      ObjectProvider<Pair<InstrumentSelector, View>> views) {
    var sdkMeterProviderBuilder = SdkMeterProvider.builder().addResource(otelResource);
    metricReaders.stream().forEach(sdkMeterProviderBuilder::registerMetricReader);
    views.stream()
        .forEach(pair -> sdkMeterProviderBuilder.registerView(pair.getFirst(), pair.getSecond()));
    return sdkMeterProviderBuilder.build();
  }

  /**
   * Creates an OpenTelemetry {@link SdkTracerProvider} with all span processors in the spring
   * context
   */
  @Bean
  @Primary
  public SdkTracerProvider terraTraceProvider(
      Resource resource, ObjectProvider<SpanProcessor> spanProcessors) {
    var tracerProviderBuilder = SdkTracerProvider.builder().setResource(resource);
    spanProcessors.stream().forEach(tracerProviderBuilder::addSpanProcessor);
    return tracerProviderBuilder.build();
  }
}
