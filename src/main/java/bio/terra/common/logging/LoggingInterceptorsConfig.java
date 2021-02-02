package bio.terra.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import(RequestIdInterceptor.class)
public class LoggingInterceptorsConfig implements WebMvcConfigurer {

  Logger log = LoggerFactory.getLogger(LoggingInterceptorsConfig.class);

  private final RequestIdInterceptor requestIdInterceptor;

  LoggingInterceptorsConfig(@Autowired RequestIdInterceptor requestIdInterceptor) {
    this.requestIdInterceptor = requestIdInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(requestIdInterceptor).order(Ordered.LOWEST_PRECEDENCE);
    log.info("Loaded logging interceptors");
  }
}
