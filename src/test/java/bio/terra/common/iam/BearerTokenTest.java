package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.*;

import bio.terra.common.stairway.test.StairwayTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
class BearerTokenTest {

  private static final Logger logger = LoggerFactory.getLogger(BearerTokenTest.class);

  private static final BearerToken TOKEN = new BearerToken("0123.456-789AbCd");

  @Test
  void equality() {
    // Positive test
    BearerToken cmp = new BearerToken(TOKEN.getToken());
    assertEquals(TOKEN, cmp);
    assertEquals(TOKEN.hashCode(), cmp.hashCode());

    // Negative tests
    assertNotEquals(TOKEN, new BearerToken("JUNK"));

    // Explicit test for comparison to self
    assertEquals(TOKEN, TOKEN);

    // Explicit test for off-type comparison
    assertNotEquals("test", TOKEN);
  }

  @Test
  void testJsonSerialization() throws JsonProcessingException {
    String asString = StairwayTestUtils.serializeToJson(TOKEN);
    logger.debug(String.format("Serialized TokenAuthenticatedRequest: '%s'", asString));
    StairwayTestUtils.validateJsonDeserialization(asString, TOKEN);
  }

  @Test
  void testJsonDeserialization() throws JsonProcessingException {
    StairwayTestUtils.validateJsonDeserialization(
        """
                    ["bio.terra.common.iam.BearerToken",{"token":"0123.456-789AbCd"}]""",
        TOKEN);
  }
}
