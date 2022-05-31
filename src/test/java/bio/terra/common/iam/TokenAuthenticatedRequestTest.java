package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.exception.UnauthorizedException;
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
public class TokenAuthenticatedRequestTest {

  private static final Logger logger = LoggerFactory.getLogger(TokenAuthenticatedRequestTest.class);

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

  private static final String TOKEN = "0123.456-789AbCd";

  @Test
  public void builder() throws Exception {
    TokenAuthenticatedRequest.Builder builder = TokenAuthenticatedRequest.builder();

    // Build fails due to no Token
    assertThrows(UnauthorizedException.class, builder::build);
    builder.setToken(TOKEN);

    // Build succeeds
    TokenAuthenticatedRequest req = builder.build();
    assertEquals(TOKEN, req.getToken());
    validateJsonSerialization(req);
  }

  @Test
  public void equality() {
    TokenAuthenticatedRequest req = TokenAuthenticatedRequest.builder().setToken(TOKEN).build();

    // Positive test
    TokenAuthenticatedRequest cmp = req.toBuilder().build();
    assertEquals(req, cmp);
    assertEquals(req.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(req, req.toBuilder().setToken("JUNK").build());

    // Explicit test for comparison to self
    assertEquals(req, req);

    // Explicit test for off-type comparison
    assertNotEquals(req, "test");
  }

  private static void validateJsonDeserialization(String json, TokenAuthenticatedRequest request)
      throws JsonProcessingException {
    TokenAuthenticatedRequest deserialized =
        objectMapper.readValue(json, TokenAuthenticatedRequest.class);
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  private static void validateJsonSerialization(TokenAuthenticatedRequest request)
      throws JsonProcessingException {
    String asString = objectMapper.writeValueAsString(request);
    logger.debug(String.format("Serialized TokenAuthenticatedRequest: '%s'", asString));
    validateJsonDeserialization(asString, request);
  }

  @Test
  public void testVectors() throws JsonProcessingException {

    validateJsonDeserialization(
        "[\"bio.terra.common.iam.TokenAuthenticatedRequest\",{\"token\":\"0123.456-789AbCd\"}]",
        TokenAuthenticatedRequest.builder().setToken("0123.456-789AbCd").build());
  }
}
