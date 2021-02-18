package bio.terra.common.tracing;

import io.opencensus.common.Scope;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.samplers.Samplers;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class TracingInterceptor implements HandlerInterceptor {

  private static final String SCOPE_KEY = "TracingInterceptor.Scope";
  private static final String SPAN_KEY = "TracingInterceptor.Span";

  @Override
  public boolean preHandle(
      HttpServletRequest httpRequest, HttpServletResponse httpResponse, Object handler) {

    String requestId = MDC.get("requestId");
    if (requestId != null) {
      Tracing.getTracer()
          .getCurrentSpan()
          .putAttribute("/terra/requestId", AttributeValue.stringAttributeValue(requestId));
    }

    if (handler instanceof HandlerMethod) {
      HandlerMethod handlerMethod = (HandlerMethod) handler;

      Map<String, AttributeValue> attributes = new HashMap<>();
      if (requestId != null) {
        attributes.put("/terra/requestId", AttributeValue.stringAttributeValue(requestId));
      }
      attributes.put("/http/method", AttributeValue.stringAttributeValue(httpRequest.getMethod()));
      attributes.put(
          "/http/path", AttributeValue.stringAttributeValue(httpRequest.getRequestURI()));

      SpanBuilder requestSpanBuilder =
          Tracing.getTracer()
              .spanBuilder(handlerMethod.getMethod().getName())
              .setSpanKind(Span.Kind.SERVER);
      requestSpanBuilder.setSampler(Samplers.alwaysSample());
      Scope scope = requestSpanBuilder.startScopedSpan();

      httpRequest.setAttribute(SCOPE_KEY, scope);
      httpRequest.setAttribute(SPAN_KEY, Tracing.getTracer().getCurrentSpan());

      RequestMapping mapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
      if (mapping != null) {
        attributes.put(
            "/http/route",
            AttributeValue.stringAttributeValue(
                Arrays.stream(mapping.path()).findFirst().orElse("unknown")));
      }

      Operation operation =
          AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Operation.class);
      if (operation != null) {
        attributes.put(
            "/terra/description", AttributeValue.stringAttributeValue(operation.summary()));

        String responseType =
            Arrays.stream(operation.responses())
                // Look for success-type responses
                .filter(response -> response.responseCode().startsWith("20"))
                // Dig through the content -> schema -> implementation to find the class type
                .flatMap(response -> Arrays.stream(response.content()))
                .map(content -> content.schema().implementation().toString())
                .findFirst()
                .orElse("n/a");
        attributes.put("/terra/responseType", AttributeValue.stringAttributeValue(responseType));
      }

      Tracing.getTracer().getCurrentSpan().putAttributes(attributes);
    }

    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    if (request.getAttribute(SPAN_KEY) != null) {
      Span span = (Span) request.getAttribute(SPAN_KEY);
      span.putAttribute(
          "/http/status_code", AttributeValue.longAttributeValue(response.getStatus()));
    }
    if (request.getAttribute(SCOPE_KEY) != null) {
      Scope scope = (Scope) request.getAttribute(SCOPE_KEY);
      scope.close();
    }
  }
}
