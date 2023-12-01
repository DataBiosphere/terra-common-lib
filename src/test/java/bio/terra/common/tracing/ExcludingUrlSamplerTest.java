package bio.terra.common.tracing;

import static org.hamcrest.MatcherAssert.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ExcludingUrlSamplerTest {
  @Test
  public void testShouldSample() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/baz", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.RECORD_AND_SAMPLE));
  }

  @Test
  public void testShouldNotSampleBecauseDelegateSaidDrop() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOff());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/baz", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  public void testShouldNotSampleByName() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/bar", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  public void testShouldNotSampleByUrlPath() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(),
                "",
                "/baz",
                SpanKind.INTERNAL,
                Attributes.of(SemanticAttributes.URL_PATH, "/bar"),
                List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  public void testShouldNotSampleByHttpTarget() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(),
                "",
                "/baz",
                SpanKind.INTERNAL,
                Attributes.of(SemanticAttributes.HTTP_TARGET, "/bar"),
                List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }
}
