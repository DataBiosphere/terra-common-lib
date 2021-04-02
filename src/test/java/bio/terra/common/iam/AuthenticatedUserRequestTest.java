package bio.terra.common.iam;

import static bio.terra.common.iam.AuthenticatedUserRequest.getMessage;
import static bio.terra.common.iam.AuthenticatedUserRequest.putMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.iam.proto.AuthenticatedUserRequestModel;
import bio.terra.common.iam.proto.AuthenticatedUserRequestModelV2;
import bio.terra.stairway.FlightMap;
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
  public void testProtoInFlightMap() {
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
   * Show backwards/forward compatibility of protobuf messages. This does not relate to
   * AuthenticatedUserRequests directly.
   *
   * <p>See
   * https://developers.google.com/protocol-buffers/docs/javatutorial#extending-a-protocol-buffer
   */
  @Test
  public void protoCompatibility() {
    AuthenticatedUserRequestModel messageV1 =
        AuthenticatedUserRequestModel.newBuilder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .build();

    AuthenticatedUserRequestModelV2 messageV2 =
        AuthenticatedUserRequestModelV2.newBuilder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setRenamedToken(TOKEN)
            .setAdditionalField("foo")
            .build();

    FlightMap inMap = new FlightMap();
    putMessage(inMap, "v1", messageV1);
    putMessage(inMap, "v2", messageV2);
    FlightMap outMap = new FlightMap();
    outMap.fromJson(inMap.toJson());
    // The protobuf binary encoding is not particularly human readable: https://developers.google.com/protocol-buffers/docs/encoding
    // ["java.util.HashMap",{"v1":"\n\u0010test@example.com\u0012\u0007Subject\u001A\u00100123.456-789AbCd","v2":"\n\u0010test@example.com\u0012\u0007Subject\u001A\u00100123.456-789AbCd\"\u0003foo"}]
    logger.info(inMap.toJson());
    // {v1=
    //test@example.comSubject0123.456-789AbCd, v2=
    //test@example.comSubject0123.456-789AbCd"foo}
    logger.info(outMap.toString());

    // Use the V2 parser to extract the V1 serialized message.
    AuthenticatedUserRequestModelV2 message1AsV2 =
        getMessage(outMap, "v1", AuthenticatedUserRequestModelV2.parser());
    assertEquals(EMAIL_ADDRESS, message1AsV2.getEmail());
    assertEquals(SUBJECT_ID, message1AsV2.getSubjectId());
    assertEquals(TOKEN, message1AsV2.getRenamedToken());
    // The new field gets the default value, the empty string.
    assertFalse(message1AsV2.hasAdditionalField());
    assertEquals("", message1AsV2.getAdditionalField());

    // Use the V1 parser to extract the V2 serialized message.
    AuthenticatedUserRequestModel message2AsV1 =
        getMessage(outMap, "v2", AuthenticatedUserRequestModel.parser());
    assertEquals(EMAIL_ADDRESS, message2AsV1.getEmail());
    assertEquals(SUBJECT_ID, message2AsV1.getSubjectId());
    assertEquals(TOKEN, message2AsV1.getToken());
  }
}
