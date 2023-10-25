package bio.terra.common.tracing;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.instrumentation.spring.autoconfigure.EnableOpenTelemetry;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring Configuration for Terra common tracing setup. */
@Configuration
@EnableOpenTelemetry
public class TracingConfig {
  @Bean
  @ConditionalOnProperty(name = "otel.sdk.disabled", havingValue = "false", matchIfMissing = true)
  public SdkTracerProvider sdkTracerProvider(Resource resource) {
    var traceExporter = TraceExporter.createWithDefaultConfiguration();

    return SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
        .setResource(resource)
        .build();
  }
}
