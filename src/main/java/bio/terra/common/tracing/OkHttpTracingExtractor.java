package bio.terra.common.tracing;

import io.opencensus.contrib.http.HttpExtractor;
import javax.annotation.Nullable;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpTracingExtractor extends HttpExtractor<Request, Response> {

  @Nullable
  @Override
  public String getRoute(Request request) {
    // OpenCensus spec wants this to be something like the literal string
    // "/api/workspace/{workspaceId}", whereas path would be "/api/workspace/12345".
    // We don't have the route available and this is a best-effort method, so use path instead.
    return request.url().encodedPath();
  }

  @Nullable
  @Override
  public String getUrl(Request request) {
    return request.url().toString();
  }

  @Nullable
  @Override
  public String getHost(Request request) {
    return request.url().host();
  }

  @Nullable
  @Override
  public String getMethod(Request request) {
    return request.method();
  }

  @Nullable
  @Override
  public String getPath(Request request) {
    return request.url().encodedPath();
  }

  @Nullable
  @Override
  public String getUserAgent(Request request) {
    return request.header("user-agent");
  }

  @Override
  public int getStatusCode(@Nullable Response response) {
    return response != null ? response.code() : 0;
  }
}
