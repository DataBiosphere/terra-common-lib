package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
public class AuthenticatedUserRequestTest {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticatedUserRequestTest.class);

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
  private static final String TOKEN = "0123.456-789AbCd";

  private static void validateJsonDeserialization(String json, AuthenticatedUserRequest request)
      throws JsonProcessingException {
    AuthenticatedUserRequest deserialized =
        objectMapper.readValue(json, AuthenticatedUserRequest.class);
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  private static void validateJsonSerialization(AuthenticatedUserRequest request)
      throws JsonProcessingException {
    String asString = objectMapper.writeValueAsString(request);
    logger.debug(String.format("Serialized AuthenticatedUserRequest: '%s'", asString));
    validateJsonDeserialization(asString, request);
  }

  @Test
  public void builder() throws Exception {
    AuthenticatedUserRequest.Builder builder = AuthenticatedUserRequest.builder();

    // Build fails due to no Email
    assertThrows(IllegalStateException.class, builder::build);
    builder.setEmail(EMAIL_ADDRESS);

    // Build fails due to no Subject
    assertThrows(IllegalStateException.class, builder::build);
    builder.setSubjectId(SUBJECT_ID);

    // Build fails due to no Token
    assertThrows(IllegalStateException.class, builder::build);
    builder.setToken(TOKEN);

    // Build succeeds
    AuthenticatedUserRequest req = builder.build();
    assertEquals(EMAIL_ADDRESS, req.getEmail());
    assertEquals(SUBJECT_ID, req.getSubjectId());
    assertEquals(TOKEN, req.getToken());
    validateJsonSerialization(req);
  }

  @Test
  public void equality() {
    AuthenticatedUserRequest req =
        AuthenticatedUserRequest.builder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .build();

    // Positive test
    AuthenticatedUserRequest cmp = req.toBuilder().build();
    assertEquals(req, cmp);
    assertEquals(req.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(req, req.toBuilder().setEmail("JUNK").build());
    assertNotEquals(req, req.toBuilder().setSubjectId("JUNK").build());
    assertNotEquals(req, req.toBuilder().setToken("JUNK").build());

    // Explicit test for comparison to self
    assertEquals(req, req);

    // Explicit test for off-type comparison
    assertNotEquals(req, "test");
  }

  @Test
  public void testVectors() throws JsonProcessingException {

    validateJsonDeserialization(
        "[\"bio.terra.common.iam.AuthenticatedUserRequest\",{\"email\":\"test@example.com\",\"reqId\":\"78f84562-c442-49be-951a-a0a56230c35f\",\"subjectId\":\"Subject\",\"token\":\"0123.456-789AbCd\"}]",
        AuthenticatedUserRequest.builder()
            .setEmail("test@example.com")
            .setSubjectId("Subject")
            .setToken("0123.456-789AbCd")
            .build());
  }
}
