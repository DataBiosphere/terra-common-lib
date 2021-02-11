package bio.terra.common.logging;

import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

class LoggingInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  public static final String TERRA_APPENDER_NAME = "terra-common";

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    ch.qos.logback.classic.Logger logbackLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    ConfigurableEnvironment environment = applicationContext.getEnvironment();

    if (Arrays.stream(environment.getActiveProfiles()).anyMatch("human-readable-logging"::equals)) {
      System.out.println("Human-readable logging enabled, skipping Google JSON layout");
      return;
    }

    logbackLogger.detachAndStopAllAppenders();
    GoogleJsonLayout layout = new GoogleJsonLayout(applicationContext);
    layout.start();

    LayoutWrappingEncoder encoder = new LayoutWrappingEncoder();
    encoder.setLayout(layout);
    encoder.start();

    ConsoleAppender appender = new ConsoleAppender();
    appender.setName(TERRA_APPENDER_NAME);
    appender.setEncoder(encoder);
    appender.setContext(logbackLogger.getLoggerContext());
    appender.start();

    logbackLogger.addAppender(appender);
  }
}
