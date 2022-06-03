package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullableModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
public class BearerTokenTest {

  private static final Logger logger = LoggerFactory.getLogger(BearerTokenTest.class);

  /**
   * ObjectMapper testing configuration
   *
   * <pre>
   * Align ObjectMapper to <a href="https://github.com/DataBiosphere/stairway/blob/develop/src/main/java/bio/terra/
   * stairway/StairwayMapper.java#L26">Stairway ObjectMapper configuration</a> in order to attempt to catch any
   * serialization incompatibilities at dev time.
   * </pre>
   */
  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new ParameterNamesModule())
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .registerModule(new JsonNullableModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

  private static final BearerToken TOKEN = new BearerToken("0123.456-789AbCd");

  @Test
  public void equality() {
    // Positive test
    BearerToken cmp = new BearerToken(TOKEN.getToken());
    assertEquals(TOKEN, cmp);
    assertEquals(TOKEN.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(TOKEN, new BearerToken("JUNK"));

    // Explicit test for comparison to self
    assertEquals(TOKEN, TOKEN);

    // Explicit test for off-type comparison
    assertNotEquals(TOKEN, "test");
  }

  private static void validateJsonDeserialization(String json, BearerToken request)
      throws JsonProcessingException {
    BearerToken deserialized = objectMapper.readValue(json, BearerToken.class);
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  private static void validateJsonSerialization(BearerToken request)
      throws JsonProcessingException {
    String asString = objectMapper.writeValueAsString(request);
    logger.debug(String.format("Serialized TokenAuthenticatedRequest: '%s'", asString));
    validateJsonDeserialization(asString, request);
  }

  @Test
  public void testVectors() throws JsonProcessingException {

    validateJsonDeserialization(
        "[\"bio.terra.common.iam.TokenAuthenticatedRequest\",{\"token\":\"0123.456-789AbCd\"}]",
        TOKEN);
  }
}
