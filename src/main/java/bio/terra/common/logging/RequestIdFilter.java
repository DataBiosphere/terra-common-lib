package bio.terra.common.logging;

import static org.apache.commons.lang3.ObjectUtils.getFirstNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hashids.Hashids;
import org.slf4j.MDC;

/**
 * A Servlet filter that ensures a requestId is extracted from an inbound HTTP request, or otherwise
 * generated randomly.
 *
 * <p>The requestId is added to the MDC context and set as a response header.
 */
@VisibleForTesting
public class RequestIdFilter implements Filter {

  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_ID_MDC_KEY = "requestId";

  private final Hashids hashids = new Hashids("requestIdSalt", 8);

  @VisibleForTesting
  public String generateRequestId() {
    return hashids.encode(ThreadLocalRandom.current().nextLong(0, Integer.MAX_VALUE));
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      HttpServletResponse httpResponse = (HttpServletResponse) response;

      String requestId =
          getFirstNonNull(() -> httpRequest.getHeader(REQUEST_ID_HEADER), this::generateRequestId);
      MDC.put(REQUEST_ID_MDC_KEY, requestId);
      httpResponse.addHeader(REQUEST_ID_HEADER, requestId);
    }

    chain.doFilter(request, response);

    MDC.remove(REQUEST_ID_MDC_KEY);
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}
}
