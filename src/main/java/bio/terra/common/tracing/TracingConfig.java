package bio.terra.common.tracing;

import io.opencensus.contrib.http.servlet.OcHttpServletFilter;
import io.opencensus.contrib.spring.aop.CensusSpringAspect;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration class for Terra common tracing setup.
 *
 * <ul>
 *   <li>{@link OcHttpServletFilter}, encloses HTTP requests to the server in a per-request
 *       OpenCensus trace.
 *   <li>{@link TracingAttributeAnnotatorInterceptor}, which adds additional per-request annotations
 *       for each service request.
 *   <li>{@link StackdriverTraceExporter} configuration, which causes traces sampled by the service
 *       to be exported to Stackdriver aka Google Cloud Tracing.
 *   <li>{@link CensusSpringAspect}, a Spring aspect to enable using the {@link
 *       io.opencensus.contrib.spring.aop.Traced} annotation on Spring bean methods to add sub
 *       spans.
 * </ul>
 *
 * <p>To enable, add this package to Spring's component scan, e.g.
 * {@code @ComponentScan(basePackages = "bio.terra.common.tracing")}
 */
@Configuration
@EnableConfigurationProperties(value = TracingProperties.class)
public class TracingConfig implements InitializingBean, WebMvcConfigurer {
  private static final Logger logger = LoggerFactory.getLogger(TracingConfig.class);

  private final ConfigurableEnvironment configurableEnvironment;
  private final TracingProperties tracingProperties;

  @Autowired
  public TracingConfig(
      ConfigurableEnvironment configurableEnvironment, TracingProperties tracingProperties) {
    this.configurableEnvironment = configurableEnvironment;
    this.tracingProperties = tracingProperties;
  }

  /**
   * A bean to register the {@link OcHttpServletFilter} to enclose server requests in OpenCensus
   * span scopes.
   */
  @Bean
  public FilterRegistrationBean<OcHttpServletFilter> tracingServletFilter() {
    FilterRegistrationBean<OcHttpServletFilter> registration =
        new FilterRegistrationBean(new OcHttpServletFilter());
    registration.setUrlPatterns(tracingProperties.getUrlPatterns());
    return registration;
  }

  @Override
  public void afterPropertiesSet() {
    setTraceSamplingProbability();
    if (tracingProperties.getStackdriverExportEnabled()) {
      try {
        // Once properties are loaded, initialize the Stackdriver exporter.
        // Use Google's default environment inspection to set options. This will attempt to extract
        // the project ID and container environment from environment variables and/or application
        // default credentials.
        StackdriverTraceExporter.createAndRegister(
            StackdriverTraceConfiguration.builder()
                // Set attributes to appear on every trace exported for this service.
                .setFixedAttributes(createFixedAttributes())
                .build());
        logger.info("Registered StackdriverTraceExporter");
      } catch (IOException e) {
        // We do not want to prevent servers from starting up if there is an error exporting traces.
        logger.error(
            "Unable to register StackdriverTraceExporter. Traces will not be exported.", e);
      }
    }
  }

  /** Propagates the workspace.tracing.probability property to the OpenCensus tracing config. */
  private void setTraceSamplingProbability() {
    TraceParams origParams = Tracing.getTraceConfig().getActiveTraceParams();
    Tracing.getTraceConfig()
        .updateActiveTraceParams(
            origParams
                .toBuilder()
                .setSampler(Samplers.probabilitySampler(tracingProperties.getSamplingRate()))
                .build());
  }

  private Map<String, AttributeValue> createFixedAttributes() {
    Map<String, AttributeValue> attributes = new HashMap<>();
    String componentName = configurableEnvironment.getProperty("spring.application.name");
    if (componentName != null) {
      attributes.put("/terra/component", AttributeValue.stringAttributeValue(componentName));
    }
    String version = configurableEnvironment.getProperty("spring.application.version");
    if (version != null) {
      attributes.put("/terra/version", AttributeValue.stringAttributeValue(version));
    }
    return attributes;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new TracingAttributeAnnotatorInterceptor());
  }

  /** Enable the @Traced annotation for use within the application. */
  @Bean
  public CensusSpringAspect censusAspect() {
    return new CensusSpringAspect(Tracing.getTracer());
  }
}