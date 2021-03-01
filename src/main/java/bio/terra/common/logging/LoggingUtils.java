package bio.terra.common.logging;

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

/**
 * Logging utility methods intended for use by service / app developers. These are generally aimed
 * around providing well-known mechanisms for supplementing logs with structured data that can be
 * read by Cloud Logging.
 */
public class LoggingUtils {

  public static final String TERRA_APPENDER_NAME = "terra-common";
  private static boolean loggingInitialized = false;

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
    if (loggingInitialized) {
      return;
    }
    loggingInitialized = true;

    ch.qos.logback.classic.Logger logbackLogger =
        (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    ConfigurableEnvironment environment = applicationContext.getEnvironment();

    if (Arrays.stream(environment.getActiveProfiles()).anyMatch("human-readable-logging"::equals)) {
      System.out.println("Human-readable logging enabled, skipping Google JSON layout");
      return;
    }

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
