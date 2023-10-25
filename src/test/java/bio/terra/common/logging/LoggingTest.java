package bio.terra.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import bio.terra.common.logging.LoggingTest.FilterTestConfiguration;
import bio.terra.common.logging.LoggingTestController.StructuredDataPojo;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import jakarta.annotation.Nullable;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests the functionality of the common logging package, including request ID generation and
 * propagation, JSON formatting, and trace / span correlation.
 *
 * <p>We use @SpringBootTest with a actual local servlet (WebEnvironment.RANDOM_PORT) to create as
 * close of an approximation to a full Spring Boot application as possible. Still, some of the
 * initialization and auto-registration of Servlet filters had to be hard-coded within the
 * ContextConfiguration annotation. See the LoggingTestApplication for an example of what an actual
 * service needs to do in order to initialize logging.
 *
 * <p>Inspired by
 * https://github.com/eugenp/tutorials/blob/master/spring-boot-modules/spring-boot-testing/src/test/java/com/baeldung/testloglevel/LogbackMultiProfileTestLogLevelIntegrationTest.java
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LoggingTestApplication.class)
@SpringJUnitConfig(
    // This is the simplest way to trigger LoggingInitializer from within this test. See
    // LoggingTestApplication for an example of how a real-world application would initialize the
    // logging flow.
    initializers = LoggingInitializer.class,
    // This line is required to cause Spring to attach the MockSpanFilter to the HttpServlet. Real
    // applications would presumably have some logic to autogeneate spans on HTTP requests, e.g.
    // io.opencensus.contrib.spring.instrument.web.HttpServletFilter.
    classes = FilterTestConfiguration.class)
// Refer to an external properties file to define a Spring application name and version, which is
// included in the JSON output if available.
@ActiveProfiles("logging-test")
@ExtendWith(OutputCaptureExtension.class)
@Tag("unit")
public class LoggingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  // Spy bean to allow us to mock out the RequestIdFilter ID generator.
  @SpyBean private RequestIdFilter requestIdFilter;

  private static SpanContext requestSpanContext;

  // A servlet filter to create an OpenCensus span at the beginning of each request
  @Component
  public static class MockSpanFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      Tracing.getTracer().spanBuilderWithExplicitParent("test-span", null).startScopedSpan();
      requestSpanContext = Tracing.getTracer().getCurrentSpan().getContext();
      chain.doFilter(request, response);
      // Note: we purposefully don't close the scope here. For some reason, it doesn't seem possible
      // to inject this filter into the servlet at high enough precedence to ensure it covers the
      // entire RequestLoggingFilter lifetime. By not closing the scope here, we can ensure that the
      // tracing span is available as context when an inbound request ultimately gets logged.
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
  }

  @TestConfiguration
  static class FilterTestConfiguration {
    @Bean
    @Order(LOWEST_PRECEDENCE)
    MockSpanFilter getMockSpanFilter() {
      return new MockSpanFilter();
    }
  }

  @BeforeEach
  public void setUp() throws IOException, ServletException {
    // Ensure the request ID is always set to a known value.
    when(requestIdFilter.generateRequestId()).thenReturn("12345");
    // Ensure the GCP project ID is always set to a known value. See
    // com.google.cloud.ServiceOptions#getDefaultProjectId for
    // lookup details.
    System.setProperty("GOOGLE_CLOUD_PROJECT", "my-project-1234");
  }

  @Test
  public void testRequestLogging(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testRequestLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String line = lastLoggedLine(capturedOutput);

    // Timestamp fields
    assertThat((Integer) readJson(line, "$.timestampSeconds")).isNotNull();
    assertThat((Integer) readJson(line, "$.timestampNanos")).isNotNull();

    // Log message & source fields
    assertThat((String) readJson(line, "$.severity")).isEqualTo("INFO");
    assertThat((String) readJson(line, "$.message")).isEqualTo("GET /testRequestLogging 200");
    assertThat((String) readJson(line, "$['logging.googleapis.com/sourceLocation'].file"))
        .isNotNull();
    assertThat((String) readJson(line, "$.serviceContext.service")).isEqualTo("loggingTest");
    assertThat((String) readJson(line, "$.serviceContext.version")).isEqualTo("1.2.3-SNAPSHOT");

    // Request-related fields
    assertThat((String) readJson(line, "$.requestId")).isEqualTo("12345");
    assertThat((String) readJson(line, "$.httpRequest.requestMethod")).isEqualTo("GET");
    assertThat((String) readJson(line, "$.httpRequest.requestUrl"))
        .isEqualTo("/testRequestLogging");
    assertThat((Integer) readJson(line, "$.httpRequest.status")).isEqualTo(200);
    // We also log all HTTP request headers. These aren't directly interpreted by Google, but are
    // available via jsonPayload.requestHeaders.*
    assertThat((Object) readJson(line, "$.requestHeaders")).isNotNull();

    // Tracing-related fields
    assertThat((String) readJson(line, "$.['logging.googleapis.com/trace']"))
        .isEqualTo(
            "projects/my-project-1234/traces/" + requestSpanContext.getTraceId().toLowerBase16());
    assertThat((String) readJson(line, "$.['logging.googleapis.com/spanId']"))
        .isEqualTo(requestSpanContext.getSpanId().toLowerBase16());
    // The JSON format will also output the trace_sampled value if that value exists in the context
    // span. Due to the way we're creating a span from scratch, there wasn't an easy way to set it
    // to true within this test.
  }

  /**
   * Tests support for structured logging from within application code. See LoggingTestController
   * for details on the invocation.
   */
  @Test
  public void testStructuredLogging(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testStructuredLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String[] lines = capturedOutput.getAll().split("\n");
    String event1 = getLogContainingMessage(lines, "Some event happened");
    String event2 = getLogContainingMessage(lines, "Another event");
    String event3 = getLogContainingMessage(lines, "Structured data");
    String event4 = getLogContainingMessage(lines, "GSON object");

    // The first log statement included a single key-value pair. Ensure that data is included in
    // the log output.
    assertThat(event1).isNotNull();
    assertThat((String) readJson(event1, "$.foo")).isEqualTo("bar");
    assertThat(event2).isNotNull();
    assertThat((Integer) readJson(event2, "$.a")).isEqualTo(1);
    assertThat((Integer) readJson(event2, "$.b")).isEqualTo(2);

    assertThat(event3).isNotNull();
    StructuredDataPojo pojo = readJson(event3, "$.pojo", StructuredDataPojo.class);
    assertThat(pojo).isNotNull();
    assertThat(pojo.name).isEqualTo("asdf");
    assertThat(pojo.id).isEqualTo(1234);

    assertThat(event4).isNotNull();
    assertThat((String) readJson(event4, "$.foo.bar")).isEqualTo("baz");
  }

  @Test
  public void testAlertLogging(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testAlertLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    String[] lines = capturedOutput.getAll().split("\n");
    String logLine = getLogContainingMessage(lines, "test alert message");
    assertThat(logLine).isNotNull();
    assertThat((String) readJson(logLine, "$.severity")).isEqualTo("ERROR");
    assertTrue((Boolean) readJson(logLine, "$." + LoggingUtils.ALERT_KEY));
  }

  @Test
  public void testAlertLoggingWithObject(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testAlertLoggingWithObject", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String[] lines = capturedOutput.getAll().split("\n");
    String logLine = getLogContainingMessage(lines, "test structured object alert message");
    StructuredDataPojo pojo = readJson(logLine, "$.pojo", StructuredDataPojo.class);
    assertThat(pojo).isNotNull();

    assertThat(logLine).isNotNull();
    assertThat((String) readJson(logLine, "$.severity")).isEqualTo("ERROR");
    assertTrue((Boolean) readJson(logLine, "$." + LoggingUtils.ALERT_KEY));
    assertThat(pojo.name).isEqualTo("asdf");
    assertThat(pojo.id).isEqualTo(1234);
  }

  // Uses the JsonPath library to extract data from a given path within a JSON string.
  @Nullable
  private <T> T readJson(String line, String path) {
    return readJson(line, path, null);
  }

  /**
   * Uses the JsonPath library to extract data from a given path within a JSON string.
   *
   * @param line The line of text to extract from
   * @param path The JSON object path to read
   * @param clazz Class to return. If null, this is inferred by the JsonPath library.
   * @return
   */
  @Nullable
  private <T> T readJson(String line, String path, @Nullable Class<T> clazz) {
    if (line.isEmpty()) {
      // JsonPath does not allow empty strings to be parsed.
      return null;
    }
    // Suppress exceptions, otherwise JsonPath will throw an exception when we look for a path that
    // doesn't exist. It's better to assert a null return value in that case.
    if (clazz != null) {
      return (T)
          JsonPath.using(
                  Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
              .parse(line)
              .read(path, clazz);
    } else {
      return (T)
          JsonPath.using(
                  Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
              .parse(line)
              .read(path);
    }
  }

  private String lastLoggedLine(CapturedOutput capturedOutput) {
    String[] lines = capturedOutput.getAll().split("\n");
    return lines[lines.length - 1];
  }

  /** Find and return the entire log line containing the provided message. */
  private String getLogContainingMessage(String[] logLines, String message) {
    return Arrays.stream(logLines)
        .filter(line -> line.contains(message))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("No log line with message " + message));
  }
}
