package bio.terra.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * An interceptor to add tracing headers to outgoing requests.
 *
 * <p>OkHttp interceptors are called before and after network requests, and can modify the request
 * and response objects. In this case, we're adding headers for the current traceId and span, which
 * the remote service can read to link traces across services.
 *
 * <p>see <a
 * href="https://opentelemetry.io/docs/instrumentation/java/manual/#context-propagation">context-propagation</a>
 * for the basis of this
 */
public class OkHttpClientTracingInterceptor implements Interceptor {
  private static final TextMapSetter<Request.Builder> SETTER =
      (carrier, key, value) -> carrier.header(key, value);

  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;

  public OkHttpClientTracingInterceptor(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    this.tracer = openTelemetry.getTracer(getClass().getName());
  }

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();

    Span requestSpan =
        tracer
            .spanBuilder(
                "%s %s".formatted(originalRequest.method(), originalRequest.url().toString()))
            .setSpanKind(SpanKind.CLIENT)
            .startSpan();

    try (Scope ignored = requestSpan.makeCurrent()) {
      // Add the attributes defined in the Semantic Conventions
      requestSpan.setAttribute(SemanticAttributes.HTTP_REQUEST_METHOD, originalRequest.method());
      requestSpan.setAttribute(SemanticAttributes.URL_SCHEME, originalRequest.url().scheme());
      requestSpan.setAttribute(SemanticAttributes.SERVER_ADDRESS, originalRequest.url().host());
      requestSpan.setAttribute(SemanticAttributes.URL_PATH, originalRequest.url().encodedPath());

      // Inject the request with the *current*  Context, which contains our current Span.
      Request.Builder newRequestBuilder = originalRequest.newBuilder();
      openTelemetry
          .getPropagators()
          .getTextMapPropagator()
          .inject(Context.current(), newRequestBuilder, SETTER);
      Request newRequest = newRequestBuilder.build();

      return chain.proceed(newRequest);
    } finally {
      requestSpan.end();
    }
  }
}
