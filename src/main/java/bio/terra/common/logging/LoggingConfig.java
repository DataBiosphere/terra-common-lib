package bio.terra.common.logging;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Spring Configuration for Terra common logging setup. This config class installs three main
 * logging-related behaviors:
 *
 * <ul>
 *   <li>RequestIdFilter, which generates or propagates a random requestId token for inbound HTTP
 *       requests, and applies requestId to the MDC and the HttpResponse.
 *   <li>RequestLoggingFilter, which collects HTTP information and geneates an info log for each
 *       inbound HTTP request.
 *   <li>GoogleJsonLayout, which installs a custom JSON logback layout with enhanced metadata for
 *       Google Cloud Logging.
 * </ul>
 */
@Configuration
public class LoggingConfig {

  private ConfigurableApplicationContext applicationContext;

  @Autowired
  public LoggingConfig(ConfigurableApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * Propagates or generates a new requestId at the beginning of our filter chain.
   *
   * <p>Highest precedence (Order(0)) so the MDC context is set as early as possible.
   */
  @Bean
  @Order(0)
  public RequestIdFilter getRequestIdFilter() {
    return new RequestIdFilter();
  }

  /**
   * Cache the request and response body using Spring's utility classes, in order to allow the
   * request logging filter to calculate the request and response size. See <a
   * href="https://www.baeldung.com/spring-reading-httpservletrequest-multiple-times">Baeldung
   * docs</a> for more info.
   *
   * <p>This needs to have higher precedence than the RequestLoggingFilter bean.
   */
  @Bean
  @Order(1)
  public RequestCacheFilter getRequestCacheFilter() {
    return new RequestCacheFilter();
  }

  /**
   * Collects and logs per-request details.
   *
   * <p>Needs to have lower precedence than the RequestCacheFilter bean.
   */
  @Bean
  @Order(2)
  public RequestLoggingFilter getRequestLoggingFilter() {
    return new RequestLoggingFilter();
  }

  /**
   * Initialized the Terra logging setup after this configuration class is constructed. This is
   * intended as a fallback measure, in case the LoggingInitializer wasn't attached to the main
   * Spring application.
   */
  @PostConstruct
  public void initLogging() {
    LoggingUtils.initializeLogging(applicationContext);
  }
}
