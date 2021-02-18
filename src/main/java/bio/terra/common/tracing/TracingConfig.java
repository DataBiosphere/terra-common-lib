package bio.terra.common.tracing;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import io.opencensus.contrib.spring.aop.CensusSpringAspect;
import io.opencensus.contrib.spring.instrument.web.client.TracingAsyncClientHttpRequestInterceptor;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ComponentScan(basePackages = "bio.terra.common.tracing")
@EnableConfigurationProperties(TracingProperties.class)
@PropertySource("classpath:common-tracing.properties")
public class TracingConfig implements InitializingBean, WebMvcConfigurer {

  private ConfigurableEnvironment configurableEnvironment;
  private TracingProperties tracingProperties;

  @Autowired
  public TracingConfig(
      ConfigurableEnvironment configurableEnvironment, TracingProperties tracingProperties) {
    this.configurableEnvironment = configurableEnvironment;
    this.tracingProperties = tracingProperties;
  }

  // Annoyingly, this bean is required for opencensus spring contrib auto-configuration. Even
  // though we don't use an async interceptor, the application context won't load successfully
  // without this bean available.
  @Bean
  public TracingAsyncClientHttpRequestInterceptor requestInterceptor() {
    return TracingAsyncClientHttpRequestInterceptor.create(null, null);
  }

  // Enable the @Traced annotation for use within the application
  @Bean
  public CensusSpringAspect censusAspect() {
    return new CensusSpringAspect(Tracing.getTracer());
  }

  // Once properties are loaded, initialize the Stackdriver exporter.
  @Override
  public void afterPropertiesSet() {
    try {
      StackdriverTraceExporter.unregister();
    } catch (IllegalStateException e) {
      System.out.println("No exporter registered yet");
    }

    if (tracingProperties.getEnabled()) {
      try {
        StackdriverTraceExporter.createAndRegister(
            StackdriverTraceConfiguration.builder()
                // Use Google's method for collecting the default project ID. This will attempt to
                // extract the project ID from environment variables and/or application default
                // credentials.
                .setProjectId(ServiceOptions.getDefaultProjectId())
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setFixedAttributes(
                    Collections.singletonMap(
                        "/terra/component",
                        AttributeValue.stringAttributeValue(
                            configurableEnvironment.getRequiredProperty(
                                "spring.application.name"))))
                .build());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // Register the Spring Web interceptor which creates a span on every inbound HTTP request.
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (tracingProperties.getEnabled()) {
      registry.addInterceptor(new TracingInterceptor());
    }
  }
}
