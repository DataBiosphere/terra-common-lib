package bio.terra.common.logging;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;

/**
 * Logging utility methods intended for use by service / app developers. These are generally aimed
 * around providing well-known mechanisms for supplementing logs with structured data that can be
 * read by Cloud Logging.
 */
public class LoggingUtils {

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
}
