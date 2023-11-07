package bio.terra.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * A filter to add tracing span around and headers to outgoing requests.
 *
 * <p>see <a
 * href="https://opentelemetry.io/docs/instrumentation/java/manual/#context-propagation">context-propagation</a>
 * for the basis of this
 */
@Provider
public class JakartaTracingFilter implements ClientRequestFilter, ClientResponseFilter {
  private static final TextMapSetter<ClientRequestContext> SETTER =
      (carrier, key, value) -> carrier.getHeaders().add(key, value);
  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;
  private Span requestSpan;

  public JakartaTracingFilter(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    this.tracer = openTelemetry.getTracer(getClass().getName());
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    this.requestSpan =
        tracer
            .spanBuilder(
                "%s %s".formatted(requestContext.getMethod(), requestContext.getUri().toString()))
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    requestSpan.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, requestContext.getMethod());
    requestSpan.setAttribute(SemanticAttributes.URL_SCHEME, requestContext.getUri().getScheme());
    requestSpan.setAttribute(SemanticAttributes.SERVER_ADDRESS, requestContext.getUri().getHost());
    requestSpan.setAttribute(SemanticAttributes.URL_PATH, requestContext.getUri().getPath());
    openTelemetry
        .getPropagators()
        .getTextMapPropagator()
        .inject(Context.current(), requestContext, SETTER);
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    this.requestSpan.end();
  }
}
