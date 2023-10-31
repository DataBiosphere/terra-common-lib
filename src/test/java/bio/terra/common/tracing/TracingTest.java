package bio.terra.common.tracing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.trace.ReadableSpan;
import java.util.Map;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/** Tests the functionality of the common tracing package. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = TracingTestApplication.class)
// Use a properties file to set tracing properties.
@ActiveProfiles("tracing-test")
@Tag("unit")
public class TracingTest {

  @Autowired private TestRestTemplate testRestTemplate;
  @Autowired private TracingTestController controller;
  @Autowired private TracedAnnotatedBean annotatedBean;

  @Test
  public void testRequestTracing() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/foo/bar", String.class);
    assertEquals(response.getStatusCode().value(), 200);
    // This relies on the opentelemetry Span implementation used to build this package. Casting to
    // this
    // implementation allows us to inspect the attributes.
    var requestSpan = (ReadableSpan) controller.getLatestSpan();
    var requestAttributeMap = requestSpan.toSpanData().getAttributes().asMap();
    Map<String, String> requestAttributes =
        requestAttributeMap.entrySet().stream()
            .collect(
                Collectors.toMap(
                    foo -> foo.getKey().getKey(), entry -> entry.getValue().toString()));

    // which keys these are is dependent on whatever open telemetry does, we just care the values
    // exist and are right
    assertThat(requestAttributes, Matchers.hasValue("200"));
    assertThat(requestAttributes, Matchers.hasValue("GET"));
    assertThat(requestAttributes, Matchers.hasValue("/foo/{id}"));
    assertThat(requestAttributes, Matchers.hasValue("/foo/bar"));
    assertThat(requestAttributes, Matchers.hasValue("localhost"));

    assertThat(requestAttributes, Matchers.hasKey("/terra/requestId"));
    assertThat(requestAttributes, Matchers.hasEntry("/terra/operationId", "getFoo"));

    var beanSpan = (ReadableSpan) annotatedBean.getLatestSpan();
    var beanSpanData = beanSpan.toSpanData();
    assertEquals("TracedAnnotatedBean.annotatedMethod", beanSpanData.getName());
    assertEquals(requestSpan.getSpanContext().getSpanId(), beanSpanData.getParentSpanId());
  }
}
