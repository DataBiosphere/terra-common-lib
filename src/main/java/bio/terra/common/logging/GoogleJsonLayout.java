package bio.terra.common.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.jackson.JacksonJsonFormatter;
import ch.qos.logback.contrib.json.JsonLayoutBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.ServiceOptions;
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
 * including so-called "special fields in JSON payloads". See <a
 * href="https://cloud.google.com/logging/docs/structured-logging#special-payload-fields">Google's
 * documentation</a> for more details.
 *
 * <p>A goal of this class is to produce maximally useful logs for operating Terra services on
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
 * that Google Cloud knows how to ingest, such as support for HttpRequest. See also <a
 * href="https://github.com/ankurcha/gcloud-logging-slf4j-logback/">gcloud-logging-slf4j-logback</a>
 * which inspired some of the patterns used here.
 */
class GoogleJsonLayout extends JsonLayoutBase<ILoggingEvent> {

  // A reference to the current Spring app context, on order to pull out the spring.application.name
  // and spring.application.version variable for inclusion in JSON output.
  private ConfigurableApplicationContext applicationContext;
  // A Jackson ObjectMapper to support converting Gson-type payloads into Jackson nodes.
  private ObjectMapper objectMapper;
  // A Logback utility class to assist with handling stack traces.
  private ThrowableProxyConverter throwableProxyConverter;
  private Tracer tracer = Tracing.getTracer();

  GoogleJsonLayout(ConfigurableApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    this.objectMapper = new ObjectMapper();
    this.throwableProxyConverter = new ThrowableProxyConverter();
    // "full" is a magic string used by the TPC to indicate we want a full stack trace, rather
    // than a truncated version.
    this.throwableProxyConverter.setOptionList(Collections.singletonList("full"));

    // Configure the superclass.
    this.appendLineSeparator = true;
    setJsonFormatter(new JacksonJsonFormatter());
  }

  /**
   * Returns a Map with properties indicating the source file and location of the code triggering
   * the logging event.
   *
   * <p>Taken largely from <a
   * href="https://github.com/ankurcha/gcloud-logging-slf4j-logback/blob/master/src/main/java/com/google/cloud/logging/GoogleCloudLoggingV2Layout.java">gcloud-logging-slf4j-logback</a>.
   */
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
  // System.err.println is OK here, since this is happening from within the logging infra.
  @SuppressWarnings("PMD.SystemPrintln")
  protected Map<String, Object> toJsonMap(ILoggingEvent event) {
    Map<String, Object> outputMap = new LinkedHashMap<>();

    outputMap.put("timestampSeconds", TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
    outputMap.put("timestampNanos", TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));

    outputMap.put("severity", String.valueOf(event.getLevel()));
    outputMap.put("message", getMessage(event));
    Map<String, Object> serviceContextMap = new HashMap<>();
    serviceContextMap.put(
        "service", applicationContext.getEnvironment().getProperty("spring.application.name"));
    serviceContextMap.put(
        "version", applicationContext.getEnvironment().getProperty("spring.application.version"));
    outputMap.put("serviceContext", serviceContextMap);

    outputMap.put("context", event.getLoggerContextVO().getName());
    outputMap.put("thread", event.getThreadName());
    outputMap.put("logger", event.getLoggerName());
    outputMap.put("logging.googleapis.com/sourceLocation", getSourceLocation(event));

    addTraceId(outputMap);
    addSpanId(outputMap);
    addTraceSampled(outputMap);

    // All MDC properties will be directly splatted onto the JSON map. This is how the MDC
    // 'requestId' property ends up in the JSON output, and ultimately into jsonPayload.requestId
    // in cloud logging.
    outputMap.putAll(event.getMDCPropertyMap());

    // Generically splat any map-like or JSON-like argument to the log call onto the output JSON.
    // This is how e.g. the RequestLoggingFilter adds the 'httpRequest' object to the JSON output.
    if (event.getArgumentArray() != null) {
      for (Object arg : event.getArgumentArray()) {
        try {
          if (arg instanceof Map) {
            // Handle arbitrary Map by splatting each key-value pair into the main output map.
            @SuppressWarnings("unchecked")
            Map<String, Object> jsonMap = (Map<String, Object>) arg;
            outputMap.putAll(jsonMap);
          } else if (arg instanceof JsonNode) {
            // Handle Jackson JsonNode by splatting each property sub-tree into the main output map.
            JsonNode jsonNode = (JsonNode) arg;
            jsonNode
                .fields()
                .forEachRemaining(entry -> outputMap.put(entry.getKey(), entry.getValue()));
          } else if (arg instanceof JsonObject) {
            // Some libraries use GSON rather than Jackson for arbitrary JSON data, and we should
            // support that too. Since we're using Jackson for top-level serialization, we need to
            // convert this gson.JsonObject into a jackson.databind.JsonNode for storage in the
            // output map. The simplest way to do this is by using a JSON string as intermediary.
            JsonObject jsonObject = (JsonObject) arg;
            JsonNode jsonNode = objectMapper.readTree(jsonObject.toString());
            jsonNode
                .fields()
                .forEachRemaining(entry -> outputMap.put(entry.getKey(), entry.getValue()));
          }
        } catch (Exception e) {
          System.err.println(String.format("Error parsing JSON: %s", e));
        }
      }
    }

    // If the generic JSON splatting above caused a 'labels' entry to exist, move the value to
    // the well-known key that Cloud Logging will ingest as proper labels key-value pairs.
    if (outputMap.containsKey("labels")) {
      outputMap.put("logging.googleapis.com/labels", outputMap.get("labels"));
      outputMap.remove("labels");
    }

    return outputMap;
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
   * Adds a Cloud Logging 'trace_sampled' attribute to the input map, only if the current context
   * has a valid span ID.
   */
  private void addTraceSampled(Map<String, Object> map) {
    SpanId spanId = tracer.getCurrentSpan().getContext().getSpanId();
    if (spanId.equals(SpanId.INVALID)) {
      return;
    }

    map.put(
        "logging.googleapis.com/trace_sampled",
        tracer.getCurrentSpan().getContext().getTraceOptions().isSampled());
  }
}
