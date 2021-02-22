package bio.terra.common.logging;

import static org.springframework.http.HttpHeaders.REFERER;
import static org.springframework.http.HttpHeaders.USER_AGENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.logging.v2.model.HttpRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * A Servlet filter that collects and logs structured information about a HTTP request and response.
 *
 * <p>The complete request and response details are logged at DEBUG level, while a short summary
 * message is logged at INFO level.
 *
 * <p>When the response is about to be returned, this class collects various request-related
 * metadata supported by the Cloud Logging API (see
 * https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry#HttpRequest). This data is
 * then sent to the logging subsystem by appending it as an additional argument to the log.info
 * call.
 */
class RequestLoggingFilter implements Filter {

  Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

  static final int MAX_PAYLOAD_TO_DEBUG_LOG = 10000;

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    if (req instanceof HttpServletRequest) {
      debugLogRequest((HttpServletRequest) req);
    }

    Instant start = Instant.now();
    chain.doFilter(req, res);
    Duration latency = Duration.between(start, Instant.now());

    if (!(req instanceof HttpServletRequest) || !(res instanceof HttpServletResponse)) {
      return;
    }
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) res;

    long requestSize = 0;
    if (request instanceof ContentCachingRequestWrapper) {
      requestSize = ((ContentCachingRequestWrapper) request).getContentAsByteArray().length;
    }
    long responseSize = 0;
    if (response instanceof ContentCachingResponseWrapper) {
      responseSize = ((ContentCachingResponseWrapper) response).getContentAsByteArray().length;
    }

    Map<String, Object> logPayload = new HashMap<>();
    logPayload.put(
        "httpRequest",
        getGoogleHttpRequestObject(request, response, latency, requestSize, responseSize));
    logPayload.put("requestHeaders", getRequestHeaders(request));

    String requestPath = null;
    try {
      URI uri = new URI(request.getRequestURI());
      requestPath = uri.getPath();
    } catch (URISyntaxException e) {
      log.error("Error parsing request path. Logging the full URI instead.", e);
      requestPath = request.getRequestURI();
    }
    // Log the message, and include the supplementary JSON as an additional arg.
    // If GoogleJsonLayout has been loaded, it will merge the JSON into the structured log output
    // for ingestion by Cloud Logging. If the default logback layout is being used, the JSON
    // argument will be ignored.
    String message =
        String.format("%s %s %s", request.getMethod(), requestPath, response.getStatus());
    log.info(message, logPayload);

    debugLogResponse(response);
  }

  /**
   * Logs various details about the inbound request at DEBUG severity.
   *
   * <p>Note that this method does not produce any structured JSON for the GoogleJsonFormat to
   * interpret. The goal is primarily to product human-readable output for local or non-production
   * debugging.
   */
  private void debugLogRequest(HttpServletRequest request) throws IOException {
    Map<String, Object> map = new HashMap<>();
    map.put("method", request.getMethod());
    map.put("uri", request.getRequestURI());

    String queryString = request.getQueryString();
    if (queryString != null) {
      map.put("query", queryString);
    }

    map.put("headers", new ServletServerHttpRequest(request).getHeaders());

    if (request instanceof ContentCachingRequestWrapper) {
      ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
      byte[] buf = wrapper.getContentAsByteArray();
      int length = Math.min(buf.length, MAX_PAYLOAD_TO_DEBUG_LOG);
      try {
        map.put("payload", new String(buf, 0, length, wrapper.getCharacterEncoding()));
      } catch (UnsupportedEncodingException e) {
        log.debug("Error reading request payload", e);
      }
    }

    log.debug(
        "REQUEST: \n{}",
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(map));
  }

  /** Logs various details about the response at DEBUG severity. */
  private void debugLogResponse(HttpServletResponse response) throws IOException {
    Map<String, Object> map = new HashMap<>();
    map.put("status", response.getStatus());

    HttpHeaders headers = new ServletServerHttpResponse(response).getHeaders();
    map.put("headers", headers);

    if (response instanceof ContentCachingResponseWrapper) {
      ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
      byte[] buf = wrapper.getContentAsByteArray();
      int length = Math.min(buf.length, MAX_PAYLOAD_TO_DEBUG_LOG);
      try {
        map.put("payload", new String(buf, 0, length, wrapper.getCharacterEncoding()));
      } catch (UnsupportedEncodingException e) {
        log.debug("Error reading response payload", e);
      }
    }
    log.debug(
        "RESPONSE: \n{}",
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(map));
  }

  /**
   * Returns a map of header key-value pairs from the input request. This takes only the first
   * header value for each key even though headers may contain multiple values. This is ultimately
   * being used for debug structured logging and querying via Cloud Logging, where it's much more
   * convenient to access a single property value than it is to dig into a single-value array.
   *
   * <p>Example query:
   *
   * <pre>jsonPayload.headers."user-agent" =~ "Chrome"</pre>
   */
  private Map<String, String> getRequestHeaders(HttpServletRequest request) {
    HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
    return headers.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, entry -> entry.getValue().stream().findFirst().orElse(null)));
  }

  /**
   * Collects metadata from the HTTP request and response and packages it into the HttpRequest POJO
   * modeled by the Cloud Logging API.
   */
  private HttpRequest getGoogleHttpRequestObject(
      HttpServletRequest request,
      HttpServletResponse response,
      Duration latency,
      long requestSize,
      long responseSize) {
    String path = "";
    try {
      URI uri = new URI(request.getRequestURI());
      path = uri.getPath();
    } catch (URISyntaxException e) {
      log.error("Error parsing request path", e);
    }

    HttpRequest httpRequest =
        new HttpRequest()
            .setLatency(String.format("%.3fs", latency.toMillis() / 1000.))
            .setProtocol(request.getProtocol())
            .setReferer(request.getHeader(REFERER))
            .setRemoteIp(request.getHeader("X-Forwarded-For"))
            .setResponseSize(responseSize)
            .setRequestMethod(request.getMethod())
            .setRequestSize(requestSize)
            // N.B. we are putting the URL path here, even though the field is called "requestUrl".
            // Cloud Logging uses this field to drive the top-level display of associated logs,
            // which
            // is very unwieldy if we include the full hostname.
            .setRequestUrl(path)
            .setStatus(response.getStatus())
            .setUserAgent(request.getHeader(USER_AGENT));
    httpRequest.setFactory(new JacksonFactory());
    return httpRequest;
  }
}
