package bio.terra.common.opentelemetry;

import bio.terra.common.tracing.ExcludingUrlSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import io.opentelemetry.sdk.metrics.export.MetricReader;
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
@EnableConfigurationProperties(value = {TracingProperties.class})
public class OpenTelemetryConfig {

  public static final Set<String> DEFAULT_EXCLUDED_URLS = Set.of("/status", "/version");

  /** Customizes the OpenTelemetry SDK sampling and adds all views and span processors. */
  @Bean
  public AutoConfigurationCustomizerProvider otelCustomizer(
      TracingProperties tracingProperties,
      ObjectProvider<Pair<InstrumentSelector, View>> views,
      ObjectProvider<MetricReader> metricReaders,
      ObjectProvider<SpanProcessor> spanProcessors) {
    return customizer -> {
      // the default exporter is the OTLP exporter, which we don't use and outputs errors like:
      // ERROR [OkHttp http://localhost:4318/...] i.o.e.internal.http.HttpExporter: Failed to export
      // metrics. The request could not be executed. Full error message: Failed to connect to
      // localhost/[0:0:0:0:0:0:0:1]:4318
      customizer.addPropertiesCustomizer((unused) -> Map.of("otel.metrics.exporter", "none"));

      customizer.addMeterProviderCustomizer(
          (builder, unused) -> {
            metricReaders.stream().forEach(builder::registerMetricReader);
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
