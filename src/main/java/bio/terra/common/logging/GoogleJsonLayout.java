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
 *   // Send arbitrary JSON to the JSON layout
 *   log.info("My Event", LoggingUtils.jsonFromString("{eventId: 'MY_EVENT'}"));
 * </pre>
 *
 * This class is similar in spirit to the StackdriverJsonLayout from the spring-gcp-logging module.
 * It removes / simplifies some config options, fixes some bugs (mostly related to tracing context
 * not being reliably included), and brings in some additional structured logging context variables
 * that Google Cloud knows how to ingest, such as support for HttpRequest. See also
 * https://github.com/ankurcha/gcloud-logging-slf4j-logback/ which inspired some of the patterns
 * used here.
 */
public class GoogleJsonLayout extends JsonLayoutBase<ILoggingEvent> {

  private Tracer tracer = Tracing.getTracer();

  // A reference to the current Spring app context, on order to pull out the spring.application.name
  // and spring.application.version variable for inclusion in JSON output.
  private ConfigurableApplicationContext applicationContext;
  // A Logback utility class to assist with handling stack traces.
  private ThrowableProxyConverter throwableProxyConverter;

  public GoogleJsonLayout(ConfigurableApplicationContext applicationContext) {
    super();

    this.applicationContext = applicationContext;
    this.throwableProxyConverter = new ThrowableProxyConverter();
    // "full" is a magic string used by the TPC to indicate we want a full stack trace, rather
    // than a truncated version.
    this.throwableProxyConverter.setOptionList(Collections.singletonList("full"));

    // Configure the superclass.
    this.appendLineSeparator = true;
    setJsonFormatter(new Gson()::toJson);
  }

  @Override
  public void start() {
    super.start();
    throwableProxyConverter.start();
  }

  @Override
  public void stop() {
    throwableProxyConverter.stop();
    super.stop();
  }

  /**
   * Converts a logging event into a Map.
   *
   * @param event the logging event
   * @return the map which should get rendered as JSON
   */
  @Override
  protected Map<String, Object> toJsonMap(ILoggingEvent event) {
    Map<String, Object> map = new LinkedHashMap<>();

    map.put("timestampSeconds", TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
    map.put("timestampNanos", TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));

    map.put("severity", String.valueOf(event.getLevel()));
    map.put("message", getMessage(event));
    map.put(
        "serviceContext",
        Map.of(
            "service", applicationContext.getEnvironment().getProperty("spring.application.name"),
            "version",
                applicationContext.getEnvironment().getProperty("spring.application.version")));

    map.put("context", event.getLoggerContextVO().getName());
    map.put("thread", event.getThreadName());
    map.put("logger", event.getLoggerName());
    map.put("logging.googleapis.com/sourceLocation", getSourceLocation(event));

    addTraceId(map);
    addSpanId(map);
    addTraceSampled(map);

    // All MDC properties will be directly splatted onto the JSON map. This is how the MDC
    // 'requestId' property ends up in the JSON output, and ultimately into jsonPayload.requestId
    // in cloud logging.
    event.getMDCPropertyMap().forEach(map::put);

    // Generically splat any map-like or JSON-like argument to the log call onto the output JSON.
    // This is how e.g. the RequestLoggingFilter adds the 'httpRequest' object to the JSON output.
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

    // If the generic JSON splatting above caused a 'labels' entry to exist, move the value to
    // the well-known key that Cloud Logging will ingest as proper labels key-value pairs.
    if (map.get("labels") != null) {
      map.put("logging.googleapis.com/labels", map.get("labels"));
      map.remove("labels");
    }

    return map;
  }

  // Pulls the log event message, and appends a stack trace if the event contains a throwable.
  String getMessage(ILoggingEvent event) {
    String message = event.getFormattedMessage();

    String stackTrace = throwableProxyConverter.convert(event);
    if (!isNullOrEmpty(stackTrace)) {
      return message + "\n" + stackTrace;
    }
    return message;
  }

  // Returns a Map with properties indicating the source file and location of the code triggering
  // the logging event.
  //
  // Taken largely from
  // https://github.com/ankurcha/gcloud-logging-slf4j-logback/blob/master/src/main/java/com/google/cloud/logging/GoogleCloudLoggingV2Layout.java
  static Map<String, Object> getSourceLocation(ILoggingEvent event) {
    StackTraceElement[] callerData = event.getCallerData();
    Map<String, Object> sourceLocation = new HashMap<>();
    if (callerData != null && callerData.length > 0) {
      StackTraceElement stackTraceElement = callerData[0];

      sourceLocation.put(
          "function",
          stackTraceElement.getClassName()
              + "."
              + stackTraceElement.getMethodName()
              + (stackTraceElement.isNativeMethod() ? "(Native Method)" : ""));
      if (stackTraceElement.getFileName() != null) {
        String packageName = stackTraceElement.getClassName().replaceAll("\\.", "/");
        packageName = packageName.substring(0, packageName.lastIndexOf("/") + 1);
        sourceLocation.put("file", packageName + stackTraceElement.getFileName());
      }
      sourceLocation.put("line", stackTraceElement.getLineNumber());
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

  /**
   * Adds a Cloud Logging traceId attribute to the input map. An entry is added only if the current
   * OpenCensus tracing context has a valid trace ID.
   */
  private void addTraceId(Map<String, Object> map) {
    TraceId traceId = tracer.getCurrentSpan().getContext().getTraceId();
    if (traceId.equals(TraceId.INVALID)) {
      return;
    }

    String projectId = ServiceOptions.getDefaultProjectId();
    if (!StringUtils.hasLength(projectId)) {
      return;
    }

    map.put(
        "logging.googleapis.com/trace",
        "projects/" + projectId + "/traces/" + traceId.toLowerBase16());
  }

  /**
   * Adds a Cloud Logging spanId attribute to the input map, only if the current context has a valid
   * span ID.
   */
  private void addSpanId(Map<String, Object> map) {
    SpanId spanId = tracer.getCurrentSpan().getContext().getSpanId();
    if (spanId.equals(SpanId.INVALID)) {
      return;
    }

    map.put("logging.googleapis.com/spanId", spanId.toLowerBase16());
  }

  /**
   * Adds a Cloud Logging 'traceSampled' attribute to the input map, only if the current context has
   * a valid span ID.
   */
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
