package bio.terra.common.tracing;

import bio.terra.common.logging.RequestIdFilter;
import io.opencensus.contrib.http.util.HttpTraceAttributeConstants;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Tracing;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A {@link HandlerInterceptor} that adds more attributes to HTTP requests being traced.
 *
 * <p>This adds more attributes that are Terra specific or could not be figured out by the {@link
 * io.opencensus.contrib.http.servlet.OcHttpServletFilter}.
 */
class TracingAttributeAnnotatorInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse, Object handler) {
    if (handler instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) handler;

      // This relies on the span for the request being already set as the current span. If there is
      // no span, this is a no-op.
      Tracing.getTracer().getCurrentSpan().putAttributes(buildAttributes(handlerMethod));
    }
    return true;
  }

  private static Map<String, AttributeValue> buildAttributes(HandlerMethod handlerMethod) {
    Map<String, AttributeValue> attributes = new HashMap<>();
    RequestMapping mapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
    if (mapping != null) {
      // The OcHttpServletFilter sets a blank HTTP route attribute. We can fill in a meaningful
      // route from our Spring RequestMapping.
      attributes.put(
          HttpTraceAttributeConstants.HTTP_ROUTE,
          AttributeValue.stringAttributeValue(
              Arrays.stream(mapping.path()).findFirst().orElse("unknown")));
    }
    String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    if (requestId != null) {
      attributes.put("/terra/requestId", AttributeValue.stringAttributeValue(requestId));
    }
    attributes.put(
        "/terra/operationId",
        AttributeValue.stringAttributeValue(handlerMethod.getMethod().getName()));
    return attributes;
  }
}
