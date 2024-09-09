package bio.terra.common.tracing;

import static org.hamcrest.MatcherAssert.assertThat;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ExcludingUrlSamplerTest {
  @Test
  void testShouldSample() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/baz", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.RECORD_AND_SAMPLE));
  }

  @Test
  void testShouldNotSampleBecauseDelegateSaidDrop() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOff());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/baz", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  void testShouldNotSampleByName() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(), "", "/bar", SpanKind.INTERNAL, Attributes.empty(), List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  void testShouldNotSampleByUrlPath() {
    // note that the internal implementation of ExcludingUrlSampler has slightly different behavior
    // when the size of the excludedUrls is smaller than the size of the urlCandidates. So this test
    // has only one excludedUrl to make sure the scenario works. The different behavior comes from
    // the implementation of Set.removeAll
    var sampler = new ExcludingUrlSampler(Set.of("/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(),
                "",
                "/baz",
                SpanKind.INTERNAL,
                Attributes.of(UrlAttributes.URL_PATH, "/bar"),
                List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }

  @Test
  void testShouldNotSampleByUrlQuery() {
    var sampler = new ExcludingUrlSampler(Set.of("/foo", "/bar"), Sampler.alwaysOn());
    assertThat(
        sampler
            .shouldSample(
                Context.root(),
                "",
                "/baz",
                SpanKind.INTERNAL,
                Attributes.of(UrlAttributes.URL_QUERY, "/bar"),
                List.of())
            .getDecision(),
        Matchers.is(SamplingDecision.DROP));
  }
}
