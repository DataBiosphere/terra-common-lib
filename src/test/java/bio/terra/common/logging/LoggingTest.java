package bio.terra.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import bio.terra.common.logging.LoggingTestController.StructuredDataPojo;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests the functionality of the common logging package, including request ID generation and
 * propagation, JSON formatting, and trace / span correlation.
 *
 * <p>We use @SpringBootTest with an actual local servlet (WebEnvironment.RANDOM_PORT) to create as
 * close of an approximation to a full Spring Boot application as possible. Still, some of the
 * initialization and auto-registration of Servlet filters had to be hard-coded within the
 * ContextConfiguration annotation. See the LoggingTestApplication for an example of what an actual
 * service needs to do in order to initialize logging.
 *
 * <p>Inspired by <a
 * href="https://github.com/eugenp/tutorials/blob/master/spring-boot-modules/spring-boot-testing-2/src/test/java/com/baeldung/testloglevel/LogbackMultiProfileTestLogLevelIntegrationTest.java"/>
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LoggingTestApplication.class)
@SpringJUnitConfig(
    // This is the simplest way to trigger LoggingInitializer from within this test. See
    // LoggingTestApplication for an example of how a real-world application would initialize the
    // logging flow.
    initializers = LoggingInitializer.class)
// Refer to an external properties file to define a Spring application name and version, which is
// included in the JSON output if available.
@ActiveProfiles("logging-test")
@ExtendWith(OutputCaptureExtension.class)
@Tag("unit")
class LoggingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  // Spy bean to allow us to mock out the RequestIdFilter ID generator.
  @SpyBean private RequestIdFilter requestIdFilter;

  @BeforeEach
  public void setUp() {
    // Ensure the request ID is always set to a known value.
    when(requestIdFilter.generateRequestId()).thenReturn("12345");
    // Ensure the GCP project ID is always set to a known value. See
    // com.google.cloud.ServiceOptions#getDefaultProjectId for
    // lookup details.
    System.setProperty("GOOGLE_CLOUD_PROJECT", "my-project-1234");
  }

  @Test
  void testRequestLogging(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testRequestLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String line = lastLoggedLine(capturedOutput);

    // Timestamp fields
    assertThat(readJson(line, "$.timestampSeconds", Integer.class)).isNotNull();
    assertThat(readJson(line, "$.timestampNanos", Integer.class)).isNotNull();

    // Log message & source fields
    assertThat((readJson(line, "$.severity", String.class))).isEqualTo("INFO");
    assertThat(readJson(line, "$.message", String.class)).isEqualTo("GET /testRequestLogging 200");
    assertThat(readJson(line, "$['logging.googleapis.com/sourceLocation'].file", String.class))
        .isNotNull();
    assertThat(readJson(line, "$.serviceContext.service", String.class)).isEqualTo("loggingTest");
    assertThat(readJson(line, "$.serviceContext.version", String.class))
        .isEqualTo("1.2.3-SNAPSHOT");

    // Request-related fields
    assertThat(readJson(line, "$.requestId", String.class)).isEqualTo("12345");
    assertThat(readJson(line, "$.httpRequest.requestMethod", String.class)).isEqualTo("GET");
    assertThat(readJson(line, "$.httpRequest.requestUrl", String.class))
        .isEqualTo("/testRequestLogging");
    assertThat(readJson(line, "$.httpRequest.status", Integer.class)).isEqualTo(200);
    // We also log all HTTP request headers. These aren't directly interpreted by Google, but are
    // available via jsonPayload.requestHeaders.*
    assertThat(readJson(line, "$.requestHeaders", Object.class)).isNotNull();

    // Tracing-related fields
    assertThat(readJson(line, "$.['logging.googleapis.com/trace']", String.class))
        .startsWith("projects/my-project-1234/traces/");
    assertThat(readJson(line, "$.['logging.googleapis.com/spanId']", String.class)).isNotBlank();
    // The JSON format will also output the trace_sampled value if that value exists in the context
    // span. Due to the way we're creating a span from scratch, there wasn't an easy way to set it
    // to true within this test.
  }

  /**
   * Tests support for structured logging from within application code. See LoggingTestController
   * for details on the invocation.
   */
  @Test
  void testStructuredLogging(CapturedOutput capturedOutput) {
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
    assertThat(readJson(event1, "$.foo", String.class)).isEqualTo("bar");
    assertThat(event2).isNotNull();
    assertThat(readJson(event2, "$.a", Integer.class)).isEqualTo(1);
    assertThat(readJson(event2, "$.b", Integer.class)).isEqualTo(2);

    assertThat(event3).isNotNull();
    StructuredDataPojo pojo = readJson(event3, "$.pojo", StructuredDataPojo.class);
    assertThat(pojo).isNotNull();
    assertThat(pojo.name).isEqualTo("asdf");
    assertThat(pojo.id).isEqualTo(1234);

    assertThat(event4).isNotNull();
    assertThat(readJson(event4, "$.foo.bar", String.class)).isEqualTo("baz");
  }

  @Test
  void testAlertLogging(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testAlertLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);
    String[] lines = capturedOutput.getAll().split("\n");
    String logLine = getLogContainingMessage(lines, "test alert message");
    assertThat(logLine).isNotNull();
    assertThat(readJson(logLine, "$.severity", String.class)).isEqualTo("ERROR");
    assertTrue(readJson(logLine, "$." + LoggingUtils.ALERT_KEY, Boolean.class));
  }

  @Test
  void testAlertLoggingWithObject(CapturedOutput capturedOutput) {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testAlertLoggingWithObject", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String[] lines = capturedOutput.getAll().split("\n");
    String logLine = getLogContainingMessage(lines, "test structured object alert message");
    StructuredDataPojo pojo = readJson(logLine, "$.pojo", StructuredDataPojo.class);
    assertThat(pojo).isNotNull();

    assertThat(logLine).isNotNull();
    assertThat(readJson(logLine, "$.severity", String.class)).isEqualTo("ERROR");
    assertTrue(readJson(logLine, "$." + LoggingUtils.ALERT_KEY, Boolean.class));
    assertThat(pojo.name).isEqualTo("asdf");
    assertThat(pojo.id).isEqualTo(1234);
  }

  /**
   * Uses the JsonPath library to extract data from a given path within a JSON string.
   *
   * @param line The line of text to extract from
   * @param path The JSON object path to read
   * @param clazz Class to return. If null, this is inferred by the JsonPath library.
   */
  private <T> T readJson(String line, String path, Class<T> clazz) {
    // Suppress exceptions, otherwise JsonPath will throw an exception when we look for a path that
    // doesn't exist. It's better to assert a null return value in that case.
    return JsonPath.using(
            Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
        .parse(line)
        .read(path, clazz);
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
