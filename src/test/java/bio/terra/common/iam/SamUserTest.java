package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.stairway.test.StairwayTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class SamUserTest {

  private static final String EMAIL_ADDRESS = "test@example.com";
  private static final String SUBJECT_ID = "Subject";
  private static final BearerToken TOKEN = new BearerToken("0123.456-789AbCd");
  private static final SamUser TEST_SAM_USER = new SamUser(EMAIL_ADDRESS, SUBJECT_ID, TOKEN);

  @Test
  void equality() {
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
  void validateJsonSerialization() throws JsonProcessingException {
    String asString = StairwayTestUtils.serializeToJson(TEST_SAM_USER);
    StairwayTestUtils.validateJsonDeserialization(asString, TEST_SAM_USER);
  }

  @Test
  void testJsonDeserialize() throws JsonProcessingException {
    StairwayTestUtils.validateJsonDeserialization(
        """
                    ["bio.terra.common.iam.SamUser",{"email":"test@example.com","subjectId":"Subject","bearerToken":["bio.terra.common.iam.BearerToken",{"token":"0123.456-789AbCd"}]}]""",
        TEST_SAM_USER);
  }
}
