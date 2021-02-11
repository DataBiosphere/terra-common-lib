package bio.terra.common.logging;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * A Servlet filter that wraps the request and response in a ContentCachingFooWrapper, to allow the
 * payloads to be read by intermediate filters, i.e. the RequestLoggingFilter.
 */
public class RequestCacheFilter implements Filter {

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
}
