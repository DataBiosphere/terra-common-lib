package bio.terra.common.tracing;

import io.opencensus.contrib.spring.aop.Traced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TracingTestController {

  private static final Logger log = LoggerFactory.getLogger(TracingTestController.class);

  @GetMapping("/testRequestTracing")
  public void testRequestLogging() {
    log.info("This is an INFO log");
    doSomeWork();
    log.info("Request handling complete");
  }

  @Traced(name = "TracingTestController.doSomeWork")
  private void doSomeWork() {
    log.info("Doing some work!");
  }
}
