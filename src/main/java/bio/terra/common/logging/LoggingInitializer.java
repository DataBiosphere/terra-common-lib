package bio.terra.common.logging;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A Spring app-context initializer for installing the GoogleJsonLayout as the sole Logback logger.
 *
 * Usage: this initializer can be added directly to a Spring application builder:
 *
 * <pre>
 *   public static void main(String[] args) throws Exception {
 *     new SpringApplicationBuilder(MyApplication.class)
 *         .initializers(new LoggingInitializer())
 *         .run(args);
 *   }
 * </pre>
 *
 * If this initializer is not attached, the LoggingAutoConfiguration class will trigger
 * initialization after the config class is constructed. In that case, the first few lines of
 * Spring logging will not be JSON-formatted, so the initializer approach is slightly preferred.
 */
public class LoggingInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    LoggingUtils.initializeLogging(applicationContext);
  }
}
