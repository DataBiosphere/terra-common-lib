package bio.terra.common.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SuppressFBWarnings(value = "UrF", justification = "Pojo fields are unread but serialized to JSON")
public class LoggingTestController {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingTestController.class);

  @GetMapping("/testRequestLogging")
  public void testRequestLogging() {
    LOG.trace("This is a TRACE log");
    LOG.debug("This is a DEBUG log");
    LOG.info("This is an INFO log");
    LOG.error("This is an ERROR log");
  }

  static class StructuredDataPojo {
    public String name;
    public int id;
  }

  @GetMapping("/testStructuredLogging")
  public void testStructuredLogging() throws JsonProcessingException {
    // Test a simple key-value pair. Should show up as {... "foo": "bar", ...}
    LOG.info("Some event happened", LoggingUtils.jsonFromString("{foo: 'bar'}"));

    // Test a map with integer values. Should show up as {... "a": 1, "b": 2, ...}
    LOG.info("Another event", LoggingUtils.jsonFromString("{a: 1, b: 2}"));

    // Test structured object serialization.
    // Should show up as {... "pojo": { "name": "asdf", "id": 1234 }, ...}
    StructuredDataPojo pojo = new StructuredDataPojo();
    pojo.name = "asdf";
    pojo.id = 1234;
    LOG.info("Structured data", LoggingUtils.structuredLogData("pojo", pojo));

    // Test that a raw GSON JsonObject works too. Some libraries such as CRL may prefer to include
    // GSON type objects in the log event payload.
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("foo", "bar");
    LOG.info("GSON object", jsonObject);
  }
}
