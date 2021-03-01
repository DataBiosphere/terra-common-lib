package bio.terra.common.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import bio.terra.common.logging.LoggingInitializer;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.export.SpanData;
import io.opencensus.trace.export.SpanExporter.Handler;
import java.util.Collection;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
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
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = TracingTestApplication.class)
@ContextConfiguration(
    // This is the simplest way to trigger LoggingInitializer from within this test. See
    // LoggingTestApplication for an example of how a real-world application would initialize the
    // logging flow.
    initializers = LoggingInitializer.class)
// Refer to an external properties file to define a Spring application name and version, which is
// included in the JSON output if available.
@ActiveProfiles("tracing-test")
@Tag("unit")
public class TracingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  // Capture stdout and stderr for log output assertions
  @Rule public OutputCaptureRule outputCapture = new OutputCaptureRule();

  @Before
  public void setUp() {
    // Ensure the GCP project ID is always set to a known value. See
    // com.google.cloud.ServiceOptions#getDefaultProjectId for
    // lookup details.
    // System.setProperty("GOOGLE_CLOUD_PROJECT", "my-project-1234");
    Tracing.getExportComponent()
        .getSpanExporter()
        .registerHandler(
            "asdf",
            new Handler() {
              @Override
              public void export(Collection<SpanData> spanDataList) {}
            });
  }

  @Test
  public void testRequestLogging() {
    ResponseEntity<String> response =
        testRestTemplate.getForEntity("/testRequestTracing", String.class);
    assertThat(response.getStatusCode().value()).isEqualTo(200);

    Tracing.getExportComponent().shutdown();
  }
}
