package bio.terra.common.tracing;

import com.google.cloud.opentelemetry.trace.TraceExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring Configuration for Terra common tracing setup. */
@Configuration
public class TracingConfig implements WebMvcConfigurer {
  @Bean
  @Primary
  @ConditionalOnProperty(
      name = "terra.common.tracing.stackdriverExportEnabled",
      havingValue = "true",
      matchIfMissing = true)
  public SdkTracerProvider googleTracerProvider(Resource resource) {
    var traceExporter = TraceExporter.createWithDefaultConfiguration();

    return SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(traceExporter).build())
        .setResource(resource)
        .build();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new RequestAttributeInterceptor());
  }
}
