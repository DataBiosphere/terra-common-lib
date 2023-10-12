package bio.terra.common.tracing;

import io.opencensus.contrib.http.HttpClientHandler;
import io.opencensus.contrib.http.HttpRequestContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.propagation.TextFormat;
import java.io.IOException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JerseyTracingFilter implements ClientRequestFilter, ClientResponseFilter {
  private static final TextFormat.Setter<ClientRequestContext> SETTER =
      new TextFormat.Setter<ClientRequestContext>() {
        @Override
        public void put(ClientRequestContext carrier, String key, String value) {
          carrier.getHeaders().add(key, value);
        }
      };

  private final Tracer tracer;
  private final HttpClientHandler<ClientRequestContext, ClientResponseContext, ClientRequestContext>
      handler;
  private HttpRequestContext opencensusRequestContext;

  public JerseyTracingFilter(Tracer tracer) {
    this.tracer = tracer;
    this.handler =
        new HttpClientHandler<>(
            tracer,
            new JerseyTracingExtractor(),
            Tracing.getPropagationComponent().getB3Format(),
            SETTER);
  }

  @Override
  public void filter(ClientRequestContext requestContext) throws IOException {
    opencensusRequestContext =
        handler.handleStart(tracer.getCurrentSpan(), requestContext, requestContext);
  }

  @Override
  public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
      throws IOException {
    handler.handleEnd(opencensusRequestContext, requestContext, responseContext, null);
  }
}
