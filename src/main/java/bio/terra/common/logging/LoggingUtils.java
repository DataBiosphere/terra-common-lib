package bio.terra.common.logging;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LoggingUtils {

  static JsonNode jsonFromString(String s) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    // Let's not be monsters here. Allow some more lenient Javascript-style JSON.
    mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(Feature.ALLOW_SINGLE_QUOTES, true);
    return mapper.readTree(s);
  }
}
