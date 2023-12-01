package bio.terra.common.opentelemetry;

import bio.terra.common.tracing.ExcludingUrlSampler;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;

@Configuration
@EnableOpenTelemetry
@EnableConfigurationProperties(value = {TracingProperties.class})
public class OpenTelemetryConfig {

  public static final Set<String> DEFAULT_EXCLUDED_URLS = Set.of("/status", "/version");

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
      Resource resource,
      ObjectProvider<SpanProcessor> spanProcessors,
      TracingProperties tracingProperties) {
    var tracerProviderBuilder = SdkTracerProvider.builder().setResource(resource);
    spanProcessors.stream().forEach(tracerProviderBuilder::addSpanProcessor);
    tracerProviderBuilder.setSampler(
        new ExcludingUrlSampler(
            Optional.ofNullable(tracingProperties.excludedUrls()).orElse(DEFAULT_EXCLUDED_URLS),
            Sampler.parentBased(Sampler.traceIdRatioBased(tracingProperties.samplingRatio()))));
    return tracerProviderBuilder.build();
  }
}
