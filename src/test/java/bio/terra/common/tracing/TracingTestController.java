package bio.terra.common.tracing;

import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** A simple REST controller to test tracing utilities with. */
@RestController
class TracingTestController {

  private static final Logger log = LoggerFactory.getLogger(TracingTestController.class);

  @Autowired TracedAnnotatedBean annotatedBean;

  /** The most recent span seen during request execution. */
  private Span latestSpan;

  @GetMapping(value = "/foo/{id}")
  public void getFoo(@PathVariable("id") String id) {
    annotatedBean.annotatedMethod();
    latestSpan = Span.current();
  }

  @GetMapping(value = "/dropme")
  public void dropme() {
    latestSpan = Span.current();
  }

  public Span getLatestSpan() {
    return latestSpan;
  }
}
