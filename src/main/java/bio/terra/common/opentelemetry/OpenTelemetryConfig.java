package bio.terra.common.opentelemetry;

import bio.terra.common.tracing.ExcludingUrlSampler;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

@Configuration
@EnableOpenTelemetry
@EnableConfigurationProperties(value = {TracingProperties.class})
public class OpenTelemetryConfig {

  public static final Set<String> DEFAULT_EXCLUDED_URLS = Set.of("/status", "/version");

  /** Customizes the OpenTelemetry SDK sampling and adds all views and span processors. */
  @Bean
  public AutoConfigurationCustomizerProvider otelCustomizer(
      TracingProperties tracingProperties,
      ObjectProvider<Pair<InstrumentSelector, View>> views,
      ObjectProvider<SpanProcessor> spanProcessors) {
    return customizer -> {
      customizer.addPropertiesCustomizer((unused) -> Map.of("otel.metrics.exporter", "none"));

      customizer.addMeterProviderCustomizer(
          (builder, unused) -> {
            views.stream().forEach(pair -> builder.registerView(pair.getFirst(), pair.getSecond()));
            return builder;
          });

      customizer.addTracerProviderCustomizer(
          (builder, unused) -> {
            spanProcessors.stream().forEach(builder::addSpanProcessor);
            builder.setSampler(
                new ExcludingUrlSampler(
                    Optional.ofNullable(tracingProperties.excludedUrls())
                        .orElse(DEFAULT_EXCLUDED_URLS),
                    Sampler.parentBased(
                        Sampler.traceIdRatioBased(tracingProperties.samplingRatio()))));
            return builder;
          });
    };
  }
}
