package bio.terra.common.logging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LoggingUtils {

  static JsonObject jsonFromString(String s) {
    return JsonParser.parseString(s).getAsJsonObject();
  }
}
