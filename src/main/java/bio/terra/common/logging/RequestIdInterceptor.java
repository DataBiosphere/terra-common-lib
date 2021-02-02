package bio.terra.common.logging;

import static org.apache.commons.lang3.ObjectUtils.getFirstNonNull;

import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hashids.Hashids;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RequestIdInterceptor implements HandlerInterceptor {

  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  public static final String REQUEST_ID_MDC_KEY = "requestId";

  private final Hashids hashids = new Hashids("requestIdSalt", 8);

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    // get an mdc id from the request (if not found, create one), and pass it along in the
    // response
    String requestId = getMdcRequestId(request);
    MDC.put(REQUEST_ID_MDC_KEY, requestId);
    response.addHeader(REQUEST_ID_HEADER, requestId);

    return true;
  }

  private String generateRequestId() {
    long generatedLong = ThreadLocalRandom.current().nextLong(0, Integer.MAX_VALUE);

    return hashids.encode(generatedLong);
  }

  private String getMdcRequestId(HttpServletRequest httpRequest) {
    return getFirstNonNull(() -> httpRequest.getHeader(REQUEST_ID_HEADER), this::generateRequestId);
  }
}
