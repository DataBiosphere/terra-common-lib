package bio.terra.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import bio.terra.common.logging.LoggingTest.FilterTestConfiguration;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import io.opencensus.common.Scope;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

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
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LoggingTestApplication.class)
@ContextConfiguration(
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
@Tag("unit")
public class LoggingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  // Spy bean to allow us to mock out the RequestIdFilter ID generator.
  @SpyBean private RequestIdFilter requestIdFilter;
  // Capture stdout and stderr for log output assertions
  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  private static SpanContext requestSpanContext;

  // A servlet filter to create an OpenCensus span at the beginning of each request
  @Component
  public static class MockSpanFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      Scope scope =
          Tracing.getTracer().spanBuilderWithExplicitParent("test-span", null).startScopedSpan();
      requestSpanContext = Tracing.getTracer().getCurrentSpan().getContext();
      chain.doFilter(request, response);
      // Note: we purposefully don't close the scope here. For some reason, it doesn't seem possible
      // to inject this filter into the servlet at high enough precedence to ensure it covers the
      // entire RequestLoggingFilter lifetime. By not closing the scope here, we can ensure that the
      // tracing span is available as context when an inbound request ultimately gets logged.
    }
  }

  @TestConfiguration
  static class FilterTestConfiguration {
    @Bean
    @Order(LOWEST_PRECEDENCE)
    MockSpanFilter getMockSpanFilter() {
      return new MockSpanFilter();
    }
  }

  @Before
  public void setUp() {
    // Ensure the request ID is always set to a known value.
    when(requestIdFilter.generateRequestId()).thenReturn("12345");
    // Ensure the GCP project ID is always set to a known value. See
    // com.google.cloud.ServiceOptions#getDefaultProjectId for
    // lookup details.
    System.setProperty("GOOGLE_CLOUD_PROJECT", "my-project-1234");
  }

  @Test
  public void testRequestLogging() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testRequestLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String line = lastLoggedLine();

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
  public void testStructuredLogging() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testStructuredLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String[] lines = outputCapture.getAll().split("\n");

    String event1 = null;
    String event2 = null;
    String event3 = null;
    for (String line : lines) {
      String message = (String) readJson(line, "$.message");
      if (message != null && message.contains("Some event happened")) {
        event1 = line;
      } else if (message != null && message.contains("Another event")) {
        event2 = line;
      } else if (message != null && message.contains("Structured data")) {
        event3 = line;
      }
    }

    // The first log statement included a single key-value pair. Ensure that data is included in
    // the log output.
    assertThat(event1).isNotNull();
    assertThat((String) readJson(event1, "$.foo")).isEqualTo("bar");
    assertThat(event2).isNotNull();
    assertThat((Integer) readJson(event2, "$.a")).isEqualTo(1);
    assertThat((Integer) readJson(event2, "$.b")).isEqualTo(2);

    assertThat(event3).isNotNull();
    assertThat((String) readJson(event3, "$.pojo.name")).isEqualTo("asdf");
    assertThat((Integer) readJson(event3, "$.pojo.id")).isEqualTo(1234);
  }

  // Uses the JsonPath library to extract data from a given path within a JSON string.
  private <T> T readJson(String line, String path) {
    // Suppress exceptions, otherwise JsonPath will throw an exception when we look for a path that
    // doesn't exist. It's better to assert a null return value in that case.
    return (T)
        JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
            .parse(line)
            .read(path);
  }

  private String lastLoggedLine() {
    String[] lines = outputCapture.getAll().split("\n");
    return lines[lines.length - 1];
  }
}
