package bio.terra.common.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Component;

/** A bean with an {@link WithSpan} annotation for testing. */
@Component
class TracedAnnotatedBean {
  /** The most recent span seen by {@link #annotatedMethod()}. */
  private Span latestSpan;

  @WithSpan
  public void annotatedMethod() {
    latestSpan = Span.current();
  }

  public Span getLatestSpan() {
    return latestSpan;
  }
}
