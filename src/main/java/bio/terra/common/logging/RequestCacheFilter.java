package bio.terra.common.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * A Servlet filter that wraps the request and response in a ContentCachingFooWrapper, to allow the
 * payloads to be read by intermediate filters, i.e. the RequestLoggingFilter.
 */
class RequestCacheFilter implements Filter {

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    ContentCachingRequestWrapper reqWrapper =
        new ContentCachingRequestWrapper((HttpServletRequest) req);
    ContentCachingResponseWrapper resWrapper =
        new ContentCachingResponseWrapper((HttpServletResponse) res);

    chain.doFilter(reqWrapper, resWrapper);
    resWrapper.copyBodyToResponse();
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}
}
