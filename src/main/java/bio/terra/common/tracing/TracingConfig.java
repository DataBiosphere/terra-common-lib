package bio.terra.common.tracing;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring Configuration for Terra common tracing setup. */
@Configuration
@ComponentScan(basePackages = "bio.terra.common.opentelemetry")
public class TracingConfig implements WebMvcConfigurer {
  private final Logger logger = LoggerFactory.getLogger(TracingConfig.class);

  /** Creates OpenTelemetry SpanProcessor that exports spans to Google Cloud Trace */
  @Bean
  @ConditionalOnProperty(
      name = "terra.common.google.tracing.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public SpanProcessor googleTracerProvider() {
    logger.info("GCP tracing enabled.");
    var traceExporter = TraceExporter.createWithDefaultConfiguration();
    return BatchSpanProcessor.builder(traceExporter).build();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RequestAttributeInterceptor());
  }
}
