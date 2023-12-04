package bio.terra.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** A filter to add tracing span around and headers to outgoing requests. */
@Provider
public class JakartaTracingFilter implements ClientRequestFilter, ClientResponseFilter {
  private static final String OPENTELEMETRY_CONTEXT = "opentelemetry.context";
  private static final String OPENTELEMETRY_SCOPE = "opentelemetry.scope";
  private static final TextMapSetter<ClientRequestContext> SETTER =
      (carrier, key, value) -> carrier.getHeaders().add(key, value);
  private final Instrumenter<ClientRequestContext, ClientResponseContext> instrumenter;

  public JakartaTracingFilter(OpenTelemetry openTelemetry) {
    this.instrumenter =
        Instrumenter.<ClientRequestContext, ClientResponseContext>builder(
                openTelemetry,
                getClass().getName(),
                request -> "%s %s".formatted(request.getMethod(), request.getUri().toString()))
            .addAttributesExtractor(
                HttpClientAttributesExtractor.create(new ClientAttributesExtractor()))
            .buildClientInstrumenter(SETTER);
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    if (instrumenter.shouldStart(Context.current(), requestContext)) {
      var otelContext = instrumenter.start(Context.current(), requestContext);
      requestContext.setProperty(OPENTELEMETRY_CONTEXT, otelContext);
      requestContext.setProperty(OPENTELEMETRY_SCOPE, otelContext.makeCurrent());
    }
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    Optional.ofNullable(requestContext.getProperty(OPENTELEMETRY_SCOPE))
        .ifPresent(scope -> ((Scope) scope).close());
    Optional.ofNullable(requestContext.getProperty(OPENTELEMETRY_CONTEXT))
        .ifPresent(
            otelContext ->
                instrumenter.end((Context) otelContext, requestContext, responseContext, null));
  }

  private static class ClientAttributesExtractor
      implements HttpClientAttributesGetter<ClientRequestContext, ClientResponseContext> {

    @Nullable
    @Override
    public String getUrlFull(ClientRequestContext clientRequestContext) {
      return clientRequestContext.getUri().toString();
    }

    @Nullable
    @Override
    public String getServerAddress(ClientRequestContext clientRequestContext) {
      return clientRequestContext.getUri().getHost();
    }

    @Nullable
    @Override
    public Integer getServerPort(ClientRequestContext clientRequestContext) {
      return clientRequestContext.getUri().getPort();
    }

    @Nullable
    @Override
    public String getHttpRequestMethod(ClientRequestContext clientRequestContext) {
      return clientRequestContext.getMethod();
    }

    @Override
    public List<String> getHttpRequestHeader(
        ClientRequestContext clientRequestContext, String name) {
      return Objects.requireNonNullElse(
          clientRequestContext.getStringHeaders().get(name), List.of());
    }

    @Nullable
    @Override
    public Integer getHttpResponseStatusCode(
        ClientRequestContext clientRequestContext,
        ClientResponseContext clientResponseContext,
        @Nullable Throwable error) {
      return clientResponseContext.getStatus();
    }

    @Override
    public List<String> getHttpResponseHeader(
        ClientRequestContext clientRequestContext,
        ClientResponseContext clientResponseContext,
        String name) {
      return Objects.requireNonNullElse(clientResponseContext.getHeaders().get(name), List.of());
    }
  }
}
