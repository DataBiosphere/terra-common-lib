package bio.terra.common.tracing;

import io.opencensus.contrib.spring.aop.Traced;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;
import org.springframework.stereotype.Component;

/** A bean with an {@link Traced} annotation for testing. */
@Component
class TracedAnnotatedBean {
  private Span latestSpan;

  @Traced
  public void annotatedMethod() {
    latestSpan = Tracing.getTracer().getCurrentSpan();
  }

  public Span getLatestSpan() {
    return latestSpan;
  }
}
