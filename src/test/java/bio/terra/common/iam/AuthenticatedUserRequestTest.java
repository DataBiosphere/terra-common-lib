package bio.terra.common.iam;

import static bio.terra.common.iam.AuthenticatedUserRequest.getJson;
import static bio.terra.common.iam.AuthenticatedUserRequest.putJson;
import static bio.terra.common.iam.AuthenticatedUserRequest.toJson;
import static bio.terra.common.iam.AuthenticatedUserRequest.toJsonV2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.stairway.FlightMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
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
  public void testVectors() throws JsonProcessingException {

    validateJsonDeserialization(
        "[\"bio.terra.common.iam.AuthenticatedUserRequest\",{\"email\":\"test@example.com\",\"reqId\":\"78f84562-c442-49be-951a-a0a56230c35f\",\"subjectId\":\"Subject\",\"token\":\"0123.456-789AbCd\"}]",
        AuthenticatedUserRequest.builder()
            .setEmail("test@example.com")
            .setSubjectId("Subject")
            .setToken("0123.456-789AbCd")
            .build());
  }

  @Test
  public void testJsonInFlightMap() {
    AuthenticatedUserRequest request =
        AuthenticatedUserRequest.builder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .build();

    FlightMap inMap = request.putIn(new FlightMap(), "foo");
    FlightMap outMap = new FlightMap();
    outMap.fromJson(inMap.toJson());

    AuthenticatedUserRequest retrieved = AuthenticatedUserRequest.getFrom(outMap, "foo");
    assertEquals(request, retrieved);
  }

  /**
   * Demonstrate backwards/forward compatibility with manual JSON serialization.
   */
  @Test
  public void jsonCompatibility() {
    AuthenticatedUserRequest request =
        AuthenticatedUserRequest.builder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .build();

    request.toRemove = AuthenticatedUserRequest.DefaultToRemove + 1;
    request.additionalField = "non-default";

    JsonNode jsonV1 = toJson(request);
    JsonNode jsonV2 = toJsonV2(request);

    FlightMap inMap = new FlightMap();
    putJson(inMap, "v1", jsonV1);
    putJson(inMap, "v2", jsonV2);
    FlightMap outMap = new FlightMap();
    outMap.fromJson(inMap.toJson());

    // ["java.util.HashMap",{"v1":"{\"email\":\"test@example.com\",\"subject_id\":\"Subject\",\"token\":\"0123.456-789AbCd\",\"to_remove\":100}","v2":"{\"email\":\"test@example.com\",\"subject_id\":\"Subject\",\"token\":\"0123.456-789AbCd\",\"additional_field\":\"non-default\"}"}]
    logger.info(inMap.toJson());

    // {v1=
    //
    // {"email":"test@example.com","subject_id":"Subject","token":"0123.456-789AbCd","to_remove":100},
    //  v2=
    //
    // {"email":"test@example.com","subject_id":"Subject","token":"0123.456-789AbCd","additional_field":"non-default"}
    //  }
    logger.info(outMap.toString());

    AuthenticatedUserRequest reqV1fromV1 =
        getJson(outMap, "v1", AuthenticatedUserRequest::fromJson);
    assertEquals(request, reqV1fromV1);
    assertEquals(request.toRemove, reqV1fromV1.toRemove);

    AuthenticatedUserRequest reqV1fromV2 =
        getJson(outMap, "v2", AuthenticatedUserRequest::fromJson);
    assertEquals(request, reqV1fromV2);
    assertEquals(AuthenticatedUserRequest.DefaultToRemove, reqV1fromV2.toRemove);

    AuthenticatedUserRequest reqV2fromV1 =
        getJson(outMap, "v1", AuthenticatedUserRequest::fromJson);
    assertEquals(request, reqV2fromV1);
    assertEquals(AuthenticatedUserRequest.DefaultAdditionalField, reqV2fromV1.additionalField);

    AuthenticatedUserRequest reqV2fromV2 =
        getJson(outMap, "v2", AuthenticatedUserRequest::fromJson);
    assertEquals(request, reqV2fromV2);
    assertEquals(request.additionalField, reqV2fromV2.additionalField);
  }
}
