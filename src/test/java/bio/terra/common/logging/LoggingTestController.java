package bio.terra.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoggingTestController {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingTestController.class);

  @GetMapping("/testRequestLogging")
  public String testRequestLogging() {
    LOG.trace("This is a TRACE log");
    LOG.debug("This is a DEBUG log");
    LOG.info("This is an INFO log");
    LOG.error("This is an ERROR log");

    return "Added some log output to console...";
  }

  @GetMapping("/testStructuredLogging")
  public String testStructuredLogging() {
    LOG.info("Some event happened", LoggingUtils.jsonFromString("{foo: 'bar'}"));
    LOG.info("Another event", LoggingUtils.jsonFromString("{a: 1, b: 2}"));

    return "Logged with arbitrary key-value pairs";
  }
}
