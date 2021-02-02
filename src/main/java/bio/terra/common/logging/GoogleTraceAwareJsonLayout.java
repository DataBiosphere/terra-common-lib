package bio.terra.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.google.cloud.ServiceOptions;
import com.google.gson.Gson;
import io.opencensus.trace.SpanId;
import io.opencensus.trace.TraceId;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.gcp.logging.StackdriverTraceConstants;
import org.springframework.util.StringUtils;

/**
 * This class provides a JSON layout for a Logback appender compatible to the Stackdriver log
 * format.
 *
 * <p>Reference: https://cloud.google.com/logging/docs/agent/configuration#process-payload
 *
 * @author Andreas Berger
 * @author Chengyuan Zhao
 * @author Stefan Dieringer
 */
public class GoogleTraceAwareJsonLayout extends JsonLayout {

  private Tracer tracer = Tracing.getTracer();

  private static final Set<String> FILTERED_MDC_FIELDS =
      new HashSet<>(
          Arrays.asList(
              StackdriverTraceConstants.MDC_FIELD_TRACE_ID,
              StackdriverTraceConstants.MDC_FIELD_SPAN_ID,
              StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT));

  private String projectId;

  private ErrorReportingServiceContext serviceContext;

  private Map<String, Object> customJson;

  /** creates a layout for a Logback appender compatible to the Stackdriver log format. */
  public GoogleTraceAwareJsonLayout() {
    this.appendLineSeparator = true;
    this.includeException = false;
    Gson formatter = new Gson();
    setJsonFormatter(formatter::toJson);
  }

  /**
   * Get the project id.
   *
   * @return the Google Cloud project id relevant for logging the traceId
   */
  public String getProjectId() {
    return this.projectId;
  }

  /**
   * set the project id.
   *
   * @param projectId the Google Cloud project id relevant for logging the traceId
   */
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  /**
   * set the service context for stackdriver.
   *
   * @param serviceContext the service context
   * @since 1.2
   */
  public void setServiceContext(ErrorReportingServiceContext serviceContext) {
    this.serviceContext = serviceContext;
  }

  /**
   * set custom json data to include in log output.
   *
   * @param json json string
   * @since 1.2
   */
  public void setCustomJson(String json) {
    Gson gson = new Gson();
    this.customJson = gson.fromJson(json, Map.class);
  }

  @Override
  public void start() {
    super.start();

    // If no Project ID set, then attempt to resolve it with the default project ID provider
    if (StringUtils.isEmpty(this.projectId) || this.projectId.endsWith("_IS_UNDEFINED")) {
      this.projectId = ServiceOptions.getDefaultProjectId();
    }
  }

  /**
   * Convert a logging event into a Map.
   *
   * @param event the logging event
   * @return the map which should get rendered as JSON
   */
  @Override
  protected Map<String, Object> toJsonMap(ILoggingEvent event) {

    Map<String, Object> map = new LinkedHashMap<>();

    if (this.includeMDC) {
      event
          .getMDCPropertyMap()
          .forEach(
              (key, value) -> {
                if (!FILTERED_MDC_FIELDS.contains(key)) {
                  map.put(key, value);
                }
              });
    }
    if (this.includeTimestamp) {
      map.put(
          StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE,
          TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
      map.put(
          StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE,
          TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));
    }

    add(
        StackdriverTraceConstants.SEVERITY_ATTRIBUTE,
        this.includeLevel,
        String.valueOf(event.getLevel()),
        map);
    add(JsonLayout.THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
    add(JsonLayout.LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);

    if (this.includeFormattedMessage) {
      String message = event.getFormattedMessage();
      IThrowableProxy throwableProxy = event.getThrowableProxy();
      if (throwableProxy != null) {
        String stackTrace = getThrowableProxyConverter().convert(event);
        if (stackTrace != null && !stackTrace.equals("")) {
          message += "\n" + stackTrace;
        }
      }
      map.put(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, message);
    }
    add(JsonLayout.MESSAGE_ATTR_NAME, this.includeMessage, event.getMessage(), map);
    add(
        JsonLayout.CONTEXT_ATTR_NAME,
        this.includeContextName,
        event.getLoggerContextVO().getName(),
        map);
    addThrowableInfo(JsonLayout.EXCEPTION_ATTR_NAME, this.includeException, event, map);

    addTraceId(map);
    addSpanId(map);

    if (this.serviceContext != null) {
      map.put(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE, this.serviceContext);
    }
    if (this.customJson != null && !this.customJson.isEmpty()) {
      for (Map.Entry<String, Object> entry : this.customJson.entrySet()) {
        map.putIfAbsent(entry.getKey(), entry.getValue());
      }
    }
    addCustomDataToJsonMap(map, event);

    return map;
  }

  protected String formatTraceId(final String traceId) {
    // Trace IDs are either 64-bit or 128-bit, which is 16-digit hex, or 32-digit hex.
    // If traceId is 64-bit (16-digit hex), then we need to prepend 0's to make a 32-digit hex.
    if (traceId != null && traceId.length() == 16) {
      return "0000000000000000" + traceId;
    }
    return traceId;
  }

  private void addTraceId(Map<String, Object> map) {
    TraceId traceId = tracer.getCurrentSpan().getContext().getTraceId();
    if (traceId.equals(TraceId.INVALID)) {
      return;
    }
    if (!StringUtils.hasLength(this.projectId)) {
      return;
    }

    add(
        StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
        true,
        StackdriverTraceConstants.composeFullTraceName(
            this.projectId, formatTraceId(traceId.toLowerBase16())),
        map);
  }

  private void addSpanId(Map<String, Object> map) {
    SpanId spanId = tracer.getCurrentSpan().getContext().getSpanId();
    if (spanId.equals(SpanId.INVALID)) {
      return;
    }

    add(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, true, spanId.toLowerBase16(), map);
  }
}
