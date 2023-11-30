package bio.terra.common.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sampler that delegates to another sampler, but excludes certain urls from sampling. See
 * https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1060#issuecomment-1711683848.
 * OpenTelemetry does not have a built-in way to exclude urls from sampling, so we have to implement
 * it ourselves.
 */
public class ExcludingUrlSampler implements Sampler {
  private final Set<String> excludedUrls;
  private final Sampler delegate;

  public ExcludingUrlSampler(Set<String> excludedUrls, Sampler delegate) {
    this.excludedUrls = excludedUrls;
    this.delegate = delegate;
  }

  @Override
  public SamplingResult shouldSample(
      Context parentContext,
      String traceId,
      String name,
      SpanKind spanKind,
      Attributes attributes,
      List<LinkData> parentLinks) {
    // name does not seem to be populated with the route at the time this is called
    // HTTP_TARGET seems to have the right information but that is deprecated
    // check URL_PATH to be forward compatible. JUST CHECK THEM ALL
    var urlCandidates = new HashSet<String>();
    urlCandidates.add(attributes.get(SemanticAttributes.URL_PATH));
    urlCandidates.add(attributes.get(SemanticAttributes.HTTP_TARGET));
    urlCandidates.add(name);

    // removeAll returns true if urlCandidates was changed meaning it contained one of the urls to
    // be excluded
    return urlCandidates.removeAll(excludedUrls)
        ? SamplingResult.drop()
        : delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
  }

  @Override
  public String getDescription() {
    return delegate.getDescription() + " excluding urls [" + String.join(", ", excludedUrls) + "]";
  }
}
