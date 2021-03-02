package bio.terra.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

/**
 * A test to verify that the human-readable-logging Spring Profile will disable the configuration of
 * Google-formatted JSON layout. This test is configured in a very similar manner to the main
 * LoggingTest, except for the @ActiveProfiles annotation.
 */
@SpringJUnitWebConfig
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = LoggingTestApplication.class)
@ContextConfiguration(initializers = LoggingInitializer.class)
@ActiveProfiles("human-readable-logging")
@Tag("unit")
@org.junit.jupiter.api.Order(20)
public class HumanReadableLoggingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  // Spy bean to allow us to mock out the RequestIdFilter ID generator.
  @SpyBean private RequestIdFilter requestIdFilter;
  // Capture stdout and stderr for log output assertions
  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  @Before
  public void setUp() {
    // Ensure the request ID is always set to a known value.
    when(requestIdFilter.generateRequestId()).thenReturn("12345");
  }

  @Test
  public void testRequestLogging() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testRequestLogging", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    String allOutput = outputCapture.getAll();

    // Normal log messages are getting passed to the logger.
    assertThat(allOutput).contains("This is an INFO log");
    assertThat(allOutput).contains("GET /testRequestLogging 200");
    // requestId is getting included via MDC and the "LOG_LEVEL_PATTERN" property specified
    // in the main logback.xml
    assertThat(allOutput).contains("12345");
    // Poor-man's check for lack of JSON
    assertThat(allOutput).doesNotContain("{");
  }
}
