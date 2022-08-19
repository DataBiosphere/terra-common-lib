package bio.terra.common.tracing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.export.SpanData;
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

  /** Converts all types of {@link AttributeValue} to String. */
  private static String coerceToString(AttributeValue attributeValue) {
    String coerced =
        attributeValue.match(
            Object::toString,
            Object::toString,
            Object::toString,
            Object::toString,
            Object::toString);
    return coerced;
  }

  @Test
  public void testRequestTracing() {
    ResponseEntity<String> response = testRestTemplate.getForEntity("/foo/bar", String.class);
    assertEquals(response.getStatusCode().value(), 200);
    // This relies on the opencensus Span implementation used to build this package. Casting to this
    // implementation allows us to inspect the attributes.
    RecordEventsSpanImpl requestSpan = (RecordEventsSpanImpl) controller.getLatestSpan();
    Map<String, AttributeValue> requestAttributeMap =
        requestSpan.toSpanData().getAttributes().getAttributeMap();
    Map<String, String> requestAttributes =
        requestAttributeMap.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> coerceToString(entry.getValue())));

    assertThat(requestAttributes, Matchers.hasEntry("http.status_code", "200"));
    assertThat(requestAttributes, Matchers.hasEntry("http.method", "GET"));
    assertThat(requestAttributes, Matchers.hasEntry("http.route", "/foo/{id}"));
    assertThat(requestAttributes, Matchers.hasEntry("http.path", "/foo/bar"));
    assertThat(requestAttributes, Matchers.hasEntry("http.host", "localhost"));
    assertThat(requestAttributes, Matchers.hasKey("http.url"));
    assertThat(requestAttributes, Matchers.hasKey("http.user_agent"));

    assertThat(requestAttributes, Matchers.hasKey("/terra/requestId"));
    assertThat(requestAttributes, Matchers.hasEntry("/terra/operationId", "getFoo"));

    RecordEventsSpanImpl beanSpan = (RecordEventsSpanImpl) annotatedBean.getLatestSpan();
    SpanData beanSpanData = beanSpan.toSpanData();
    assertEquals("annotatedMethod", beanSpanData.getName());
    assertEquals(requestSpan.getContext().getSpanId(), beanSpanData.getParentSpanId());
  }
}
