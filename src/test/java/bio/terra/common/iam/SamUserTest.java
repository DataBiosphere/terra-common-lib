package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
public class SamUserTest {

  private static final Logger logger = LoggerFactory.getLogger(SamUserTest.class);

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
  private static final BearerToken TOKEN = new BearerToken("0123.456-789AbCd");
  private static final SamUser TEST_SAM_USER = new SamUser(EMAIL_ADDRESS, SUBJECT_ID, TOKEN);

  private static void validateJsonDeserialization(String json, SamUser request)
      throws JsonProcessingException {
    SamUser deserialized = objectMapper.readValue(json, SamUser.class);
    assertEquals(request, deserialized);
    assertEquals(request.hashCode(), deserialized.hashCode());
  }

  @Test
  public void equality() {
    // Positive test
    SamUser cmp = new SamUser(EMAIL_ADDRESS, SUBJECT_ID, TOKEN);
    assertEquals(TEST_SAM_USER, cmp);
    assertEquals(TEST_SAM_USER.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(TEST_SAM_USER, new SamUser("JUNK", SUBJECT_ID, TOKEN));
    assertNotEquals(TEST_SAM_USER, new SamUser(EMAIL_ADDRESS, "JUNK", TOKEN));
    assertNotEquals(TEST_SAM_USER, new SamUser(EMAIL_ADDRESS, SUBJECT_ID, new BearerToken("JUNK")));

    // Explicit test for comparison to self
    assertEquals(TEST_SAM_USER, TEST_SAM_USER);

    // Explicit test for off-type comparison
    assertNotEquals(TEST_SAM_USER, "test");
  }

  @Test
  public void validateJsonSerialization() throws JsonProcessingException {
    String asString = objectMapper.writeValueAsString(TEST_SAM_USER);
    logger.debug(String.format("Serialized SamUserAuthenticatedRequest: '%s'", asString));
    validateJsonDeserialization(asString, TEST_SAM_USER);
  }

  @Test
  public void testJsonDeserialize() throws JsonProcessingException {
    validateJsonDeserialization(
        "[\"bio.terra.common.iam.SamUser\",{\"email\":\"test@example.com\",\"subjectId\":\"Subject\",\"bearerToken\":[\"bio.terra.common.iam.BearerToken\",{\"token\":\"0123.456-789AbCd\"}]}]",
        TEST_SAM_USER);
  }
}
