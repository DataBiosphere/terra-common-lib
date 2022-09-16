package bio.terra.common.tracing;

import io.opencensus.contrib.http.HttpExtractor;
import javax.annotation.Nullable;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

/**
 * Extractor to populate span fields from a Request object
 *
 * <p>OpenCensus spans have a number of fields defined, this class extracts values from Jersey
 * request/response contexts to populate those fields. Per the OpenCensus spec, "All attributes are
 * optional, but collector should make the best effort to collect those."
 */
public class JerseyTracingExtractor
    extends HttpExtractor<ClientRequestContext, ClientResponseContext> {

  @Nullable
  @Override
  public String getRoute(ClientRequestContext request) {
    // OpenCensus spec wants this to be something like the literal string
    // "/api/workspace/{workspaceId}", whereas path would be "/api/workspace/12345".
    // We don't have the route available and this is an optional method, so return null.
    return null;
  }

  @Nullable
  @Override
  public String getUrl(ClientRequestContext request) {
    return request.getUri().toString();
  }

  @Nullable
  @Override
  public String getHost(ClientRequestContext request) {
    return request.getUri().getHost();
  }

  @Nullable
  @Override
  public String getMethod(ClientRequestContext request) {
    return request.getMethod();
  }

  @Nullable
  @Override
  public String getPath(ClientRequestContext request) {
    return request.getUri().getPath();
  }

  @Nullable
  @Override
  public String getUserAgent(ClientRequestContext request) {
    return request.getHeaderString("user-agent");
  }

  @Override
  public int getStatusCode(@Nullable ClientResponseContext response) {
    // Per base class, "If the response is null, this method should return 0".
    return response != null ? response.getStatus() : 0;
  }
}
