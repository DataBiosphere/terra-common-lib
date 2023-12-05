package bio.terra.common.tracing;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.io.IOException;
import java.util.List;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** An interceptor to add tracing span around and headers to outgoing requests. */
public class OkHttpClientTracingInterceptor implements Interceptor {
  private static final TextMapSetter<Request.Builder> SETTER =
      (carrier, key, value) -> carrier.header(key, value);

  private final Instrumenter<Request.Builder, Response> instrumenter;

  public OkHttpClientTracingInterceptor(OpenTelemetry openTelemetry) {
    this.instrumenter =
        Instrumenter.<Request.Builder, Response>builder(
                openTelemetry,
                getClass().getName(),
                request -> {
                  var builtReq = request.build();
                  return "%s %s".formatted(builtReq.method(), builtReq.url());
                })
            .addAttributesExtractor(
                HttpClientAttributesExtractor.create(new ClientAttributesExtractor()))
            .buildClientInstrumenter(SETTER);
  }

  @NotNull
  @Override
  public Response intercept(Chain chain) throws IOException {
    // we have to use a request builder because the request itself is immutable
    // the instrumenter methods modify the request builder in place, adding headers
    var requestBuilder = chain.request().newBuilder();
    if (instrumenter.shouldStart(Context.current(), requestBuilder)) {
      Context context = instrumenter.start(Context.current(), requestBuilder);
      try (Scope ignored = context.makeCurrent()) {
        var response = chain.proceed(requestBuilder.build());
        instrumenter.end(context, requestBuilder, response, null);
        return response;
      } catch (Exception e) {
        instrumenter.end(context, requestBuilder, null, e);
        throw e;
      }
    } else {
      return chain.proceed(chain.request());
    }
  }

  private static class ClientAttributesExtractor
      implements HttpClientAttributesGetter<Request.Builder, Response> {

    @Nullable
    @Override
    public String getHttpRequestMethod(Request.Builder builder) {
      return builder.build().method();
    }

    @Override
    public List<String> getHttpRequestHeader(Request.Builder builder, String name) {
      return builder.build().headers(name);
    }

    @Nullable
    @Override
    public Integer getHttpResponseStatusCode(
        Request.Builder builder, Response response, @Nullable Throwable error) {
      return response.code();
    }

    @Override
    public List<String> getHttpResponseHeader(
        Request.Builder builder, Response response, String name) {
      return response.headers(name);
    }

    @Nullable
    @Override
    public String getUrlFull(Request.Builder builder) {
      return builder.build().url().toString();
    }

    @Nullable
    @Override
    public String getServerAddress(Request.Builder builder) {
      return builder.build().url().host();
    }

    @Nullable
    @Override
    public Integer getServerPort(Request.Builder builder) {
      return builder.build().url().port();
    }
  }
}
