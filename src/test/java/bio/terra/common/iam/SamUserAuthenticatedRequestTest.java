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
public class SamUserAuthenticatedRequestTest {

  private static final Logger logger =
      LoggerFactory.getLogger(SamUserAuthenticatedRequestTest.class);

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

  private static final String EMAIL_ADDRESS = "test@example.com";
  private static final String SUBJECT_ID = "Subject";
  private static final TokenAuthenticatedRequest TOKEN =
      new TokenAuthenticatedRequest.Builder().setToken("0123.456-789AbCd").build();

  @Test
  public void builder() throws Exception {
    SamUserAuthenticatedRequest.Builder builder = SamUserAuthenticatedRequest.builder();

    // Build fails due to no Email
    assertThrows(UnauthorizedException.class, builder::build);
    builder.setEmail(EMAIL_ADDRESS);

    // Build fails due to no Subject
    assertThrows(UnauthorizedException.class, builder::build);
    builder.setSubjectId(SUBJECT_ID);

    // Build fails due to no Token
    assertThrows(UnauthorizedException.class, builder::build);
    builder.setTokenRequest(TOKEN);

    // Build succeeds
    SamUserAuthenticatedRequest req = builder.build();
    assertEquals(EMAIL_ADDRESS, req.getEmail());
    assertEquals(SUBJECT_ID, req.getSubjectId());
    assertEquals(TOKEN, req.getTokenRequest());
    validateJsonSerialization(req);
  }

  @Test
  public void equality() {
    SamUserAuthenticatedRequest req =
        SamUserAuthenticatedRequest.builder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setTokenRequest(TOKEN)
            .build();

    // Positive test
    SamUserAuthenticatedRequest cmp = req.toBuilder().build();
    assertEquals(req, cmp);
    assertEquals(req.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(req, req.toBuilder().setEmail("JUNK").build());
    assertNotEquals(req, req.toBuilder().setSubjectId("JUNK").build());
    assertNotEquals(
        req,
        req.toBuilder()
            .setTokenRequest(new TokenAuthenticatedRequest.Builder().setToken("JUNK").build())
            .build());

    // Explicit test for comparison to self
    assertEquals(req, req);

    // Explicit test for off-type comparison
    assertNotEquals(req, "test");
  }

  private static void validateJsonDeserialization(String json, SamUserAuthenticatedRequest request)
      throws JsonProcessingException {
    SamUserAuthenticatedRequest deserialized =
        objectMapper.readValue(json, SamUserAuthenticatedRequest.class);
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  private static void validateJsonSerialization(SamUserAuthenticatedRequest request)
      throws JsonProcessingException {
    String asString = objectMapper.writeValueAsString(request);
    logger.debug(String.format("Serialized SamUserAuthenticatedRequest: '%s'", asString));
    validateJsonDeserialization(asString, request);
  }

  @Test
  public void testVectors() throws JsonProcessingException {

    validateJsonDeserialization(
        "[\"bio.terra.common.iam.SamUserAuthenticatedRequest\",{\"email\":\"test@example.com\",\"subjectId\":\"Subject\",\"tokenRequest\":[\"bio.terra.common.iam.TokenAuthenticatedRequest\",{\"token\":\"0123.456-789AbCd\"}]}]",
        SamUserAuthenticatedRequest.builder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setTokenRequest(TOKEN)
            .build());
  }
}
