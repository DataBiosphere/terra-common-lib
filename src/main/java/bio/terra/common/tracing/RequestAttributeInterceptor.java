package bio.terra.common.tracing;

import bio.terra.common.logging.RequestIdFilter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * A {@link HandlerInterceptor} that adds more tracing attributes to HTTP requests.
 *
 * <p>This class expects the request to already have a span set as the current context.
 *
 * <p>This adds attributes that are Terra specific.
 */
class RequestAttributeInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse, Object handler) {
    if (handler instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) handler;
      // This relies on the span for the request being already set as the current span. If there is
      // no span, this is a no-op.
      Span.current().setAllAttributes(buildAttributes(handlerMethod));
    }
    return true;
  }

  private Attributes buildAttributes(HandlerMethod handlerMethod) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    // We expect the RequestIdFilter to have already set up the MDC context. This is true today
    // because servlet Filters are always run before Spring HandlerInterceptors.
    String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
    if (requestId != null) {
      attributesBuilder.put("/terra/requestId", requestId);
    }
    // The "operationId" comes from the "operationId" field defined on the OpenAPI 3 definition.
    attributesBuilder.put("/terra/operationId", handlerMethod.getMethod().getName());
    return attributesBuilder.build();
  }
}
