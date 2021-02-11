package bio.terra.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** */
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

  Logger log = LoggerFactory.getLogger(LoggingConfig.class);

  // Propagate or generate a new requestId at the beginning of our filter chain.
  @Bean
  @Order(0)
  public RequestIdFilter getRequestIdFilter() {
    return new RequestIdFilter();
  }

  // Cache the request and response body using Spring's utility classes, in order to allow
  // the request logging filter to calculate the request and response size.
  // See https://www.baeldung.com/spring-reading-httpservletrequest-multiple-times
  @Bean
  @Order(1)
  public RequestCacheFilter getRequestCacheFilter() {
    return new RequestCacheFilter();
  }

  // Finally, collect and log request details.
  @Bean
  @Order(2)
  public RequestLoggingFilter getRequestLoggingFilter() {
    return new RequestLoggingFilter();
  }
}
