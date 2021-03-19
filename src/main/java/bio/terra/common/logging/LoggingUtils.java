package bio.terra.common.logging;

import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ResourceUtils;

/**
 * Logging utility methods intended for use by service / app developers. These are generally aimed
 * around providing well-known mechanisms for supplementing logs with structured data that can be
 * read by Cloud Logging.
 */
public class LoggingUtils {

  public static final String TERRA_APPENDER_NAME = "terra-common";

  /**
   * Parses a JSON string and returns a Jackson JsonNode object which can be passed as an argument
   * for inclusion in JSON logging output.
   *
   * <p>Example usage:
   *
   * <pre>
   *   log.info("My message", LoggingUtils.jsonFromString("{eventType: 'very-rare-event'}"));
   *
   *   // Results in an output log of the form:
   *   {... "eventType": "very-rare-event", ...}
   *
   *   // which lands in Cloud Logging via the 'jsonPayload' field, accessible by querying:
   *   jsonPayload.eventType = "very-rare-event"
   * </pre>
   */
  static JsonNode jsonFromString(String s) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    // Let's not be monsters here. Allow some more lenient Javascript-style JSON.
    mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
    return mapper.readTree(s);
  }

  /**
   * Adds an arbitrary key-value pair for inclusion in JSON logging output. The 'value' Object may
   * be any POJO that can be serialized by Jackson.
   *
   * <p>Example usage:
   *
   * <pre>
   *   log.info("My message", LoggingUtils.structuredLogData("event", myEventObject));
   *
   *   // Results in an output log of the form:
   *   {... "event": { [serialized version of myEventObject] }, ...}
   *
   *   // which lands in Cloud Logging via the 'jsonPayload' field, accessible by querying:
   *   jsonPayload.event.subfield.value = "12345"
   * </pre>
   */
  static Map<String, Object> structuredLogData(String key, Object value) {
    return Collections.singletonMap(key, value);
  }

  /**
   * Initializes the Terra logging configuration, primarily by installing GoogleJsonLayout as the
   * sole Logback logger.
   *
   * <p>This method will only apply configuration once; subsequent calls will have no effect.
   *
   * <p>If the "human-readable-logging" Spring profile is active, no changes will be made and the
   * default Spring logging config (see resources/logback.xml) will be used.
   */
  protected static void initializeLogging(ConfigurableApplicationContext applicationContext) {
    ch.qos.logback.classic.Logger logbackLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    ConfigurableEnvironment environment = applicationContext.getEnvironment();

    if (Arrays.stream(environment.getActiveProfiles()).anyMatch("human-readable-logging"::equals)) {
      System.out.println("Human-readable logging enabled, re-applying original logback.xml config");
      try {
        // Note: there is some nuance in how best to reset the logback context. This code path is
        // only encountered in (1) unit tests, where we actively need to reset the context since
        // prior tests may have enabled the JSON layout, and (2) local testing of a service, where
        // we want to disrupt as little of the original logging config as possible (e.g. Spring-
        // configured log levels via "logging.level.foo.bar=DEBUG".
        //
        // Empirically, detaching all appenders and reloading the context via this logback file
        // seems to work. But if we encounter future issues in human-readable logging control, this
        // is a reasonable place to look more closely.
        logbackLogger.detachAndStopAllAppenders();
        new ContextInitializer(logbackLogger.getLoggerContext())
            .configureByResource(ResourceUtils.getURL("classpath:logback.xml"));
      } catch (Exception e) {
        throw new RuntimeException("Error loading human-readable logging", e);
      }
    } else {
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

      logbackLogger.detachAndStopAllAppenders();
      logbackLogger.addAppender(appender);
    }
  }
}
