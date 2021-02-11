package bio.terra.common.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import com.google.cloud.ServiceOptions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.opencensus.trace.SpanId;
import io.opencensus.trace.TraceId;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.gcp.logging.StackdriverErrorReportingServiceContext;
import org.springframework.cloud.gcp.logging.StackdriverTraceConstants;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * A Logback JSON layout aimed at generating log lines with useful details for ingestion by the
 * Google Cloud Logging system.
 *
 * <p>See the LoggingTest integration test for more details on the output structure, including
 * concrete examples of field values.
 *
 * <p>The output JSON relies heavily on Google Cloud's interpretation of structured JSON logs,
 * including so-called "special fields in JSON payloads". See
 * https://cloud.google.com/logging/docs/structured-logging#special-payload-fields for more details.
 *
 * <p>A goal of this class is to product maximally useful logs for operating Terra services on
 * Google Cloud, including correlation with trace details and inclusion of useful metadata such as
 * HTTP request info and MDC key-value pairs.
 *
 * <p>One interesting usage pattern enabled by this layout is the ability for service code to
 * generate structured logs with very minimal boilerplate. Any arguments of the log event which are
 * JSON-like will have their keys included in the output JSON. Google, in turn, will ingest that
 * data as part of the "jsonPayload" field in the logging API. This facilitates more fine-grained
 * log searches, and can even be used to drive logs-based metrics and alerting.
 *
 * <p>Example usage:
 *
 * <pre>
 *   log.info(
 *
 * </pre>
 */
public class GoogleJsonLayout extends JsonLayoutBase<ILoggingEvent> {

  private Tracer tracer = Tracing.getTracer();

  private String projectId;
  private ConfigurableApplicationContext applicationContext;
  private ThrowableProxyConverter throwableProxyConverter;

  public GoogleJsonLayout(ConfigurableApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    this.throwableProxyConverter = new ThrowableProxyConverter();
    this.throwableProxyConverter.setOptionList(Collections.singletonList("full"));

    // Configure the superclass.
    this.appendLineSeparator = true;
    setJsonFormatter(new Gson()::toJson);
  }

  @Override
  public void start() {
    super.start();
    throwableProxyConverter.start();

    // this.projectId = ServiceOptions.getDefaultProjectId();
  }

  @Override
  public void stop() {
    throwableProxyConverter.stop();
    super.stop();
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

    map.put(
        StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE,
        TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
    map.put(
        StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE,
        TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));

    map.put("severity", String.valueOf(event.getLevel()));
    map.put("message", getMessage(event));
    map.put(
        "serviceContext",
        new StackdriverErrorReportingServiceContext(
            applicationContext.getEnvironment().getProperty("spring.application.name"),
            applicationContext.getEnvironment().getProperty("spring.application.version")));

    map.put("context", event.getLoggerContextVO().getName());
    map.put("thread", event.getThreadName());
    map.put("logger", event.getLoggerName());
    map.put("logging.googleapis.com/sourceLocation", getSourceLocation(event));

    addTraceId(map);
    addSpanId(map);
    addTraceSampled(map);

    event.getMDCPropertyMap().forEach(map::put);

    if (event.getArgumentArray() != null) {
      for (Object arg : event.getArgumentArray()) {
        if (arg instanceof Map) {
          Map<String, Object> jsonMap = (Map<String, Object>) arg;
          jsonMap.forEach(map::put);
        } else if (arg instanceof JsonObject) {
          JsonObject jsonObject = (JsonObject) arg;
          jsonObject.entrySet().forEach(entry -> map.put(entry.getKey(), entry.getValue()));
        }
      }
    }

    if (map.get("labels") != null) {
      map.put("logging.googleapis.com/labels", map.get("labels"));
      map.remove("labels");
    }

    return map;
  }

  String getMessage(ILoggingEvent event) {
    String message = event.getFormattedMessage();

    String stackTrace = throwableProxyConverter.convert(event);
    if (!isNullOrEmpty(stackTrace)) {
      return message + "\n" + stackTrace;
    }
    return message;
  }

  // Taken from
  // https://github.com/ankurcha/gcloud-logging-slf4j-logback/blob/master/src/main/java/com/google/cloud/logging/GoogleCloudLoggingV2Layout.java
  static Map<String, Object> getSourceLocation(ILoggingEvent event) {
    StackTraceElement[] cda = event.getCallerData();
    Map<String, Object> sourceLocation = new HashMap<>();
    if (cda != null && cda.length > 0) {
      StackTraceElement ste = cda[0];

      sourceLocation.put(
          "function",
          ste.getClassName()
              + "."
              + ste.getMethodName()
              + (ste.isNativeMethod() ? "(Native Method)" : ""));
      if (ste.getFileName() != null) {
        String pkg = ste.getClassName().replaceAll("\\.", "/");
        pkg = pkg.substring(0, pkg.lastIndexOf("/") + 1);
        sourceLocation.put("file", pkg + ste.getFileName());
      }
      sourceLocation.put("line", ste.getLineNumber());
    } else {
      sourceLocation.put("file", CallerData.NA);
      sourceLocation.put("line", CallerData.LINE_NA);
      sourceLocation.put("function", CallerData.NA);
    }
    return sourceLocation;
  }

  private static boolean isNullOrEmpty(String string) {
    return string == null || string.length() == 0;
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
    System.out.println(traceId.toString());
    if (traceId.equals(TraceId.INVALID)) {
      return;
    }

    String projectId = ServiceOptions.getDefaultProjectId();
    if (!StringUtils.hasLength(projectId)) {
      return;
    }

    map.put(
        StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
        StackdriverTraceConstants.composeFullTraceName(
            projectId, formatTraceId(traceId.toLowerBase16())));
  }

  private void addSpanId(Map<String, Object> map) {
    SpanId spanId = tracer.getCurrentSpan().getContext().getSpanId();
    System.out.println(spanId.toString());
    if (spanId.equals(SpanId.INVALID)) {
      return;
    }

    map.put(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, spanId.toLowerBase16());
  }

  private void addTraceSampled(Map<String, Object> map) {
    SpanId spanId = tracer.getCurrentSpan().getContext().getSpanId();
    if (spanId.equals(SpanId.INVALID)) {
      return;
    }

    map.put(
        "logging.googleapis.com/traceSampled",
        tracer.getCurrentSpan().getContext().getTraceOptions().isSampled());
  }
}
