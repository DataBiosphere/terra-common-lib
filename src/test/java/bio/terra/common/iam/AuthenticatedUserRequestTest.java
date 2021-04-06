package bio.terra.common.iam;

import static bio.terra.common.iam.AuthenticatedUserRequest.getRecord;
import static bio.terra.common.iam.AuthenticatedUserRequest.putRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import bio.terra.common.iam.avro.AuthenticatedUserRequestModel;
import bio.terra.common.iam.avro.AuthenticatedUserRequestModelV2;
import bio.terra.stairway.FlightMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
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
  public void testProtoInFlightMap() throws IOException {
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
  public void avroCompatibility() throws IOException {
    AuthenticatedUserRequestModel messageV1 =
        AuthenticatedUserRequestModel.newBuilder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .setToRemove(44)
            .build();

    AuthenticatedUserRequestModelV2 messageV2 =
        AuthenticatedUserRequestModelV2.newBuilder()
            .setEmail(EMAIL_ADDRESS)
            .setSubjectId(SUBJECT_ID)
            .setToken(TOKEN)
            .setAdditionalField("foo")
            .build();

    FlightMap inMap = new FlightMap();
    putRecord(
        inMap,
        "v1",
        AuthenticatedUserRequestModel.getClassSchema(),
        messageV1,
        AuthenticatedUserRequestModel.class);
    putRecord(
        inMap,
        "v2",
        AuthenticatedUserRequestModelV2.getClassSchema(),
        messageV2,
        AuthenticatedUserRequestModelV2.class);
    FlightMap outMap = new FlightMap();
    outMap.fromJson(inMap.toJson());

    // Avro JSON encoding (stored as a string in map
    // ["java.util.HashMap",{"v1":["java.util.Arrays$ArrayList",["{\"type\":\"record\",\"name\":\"AuthenticatedUserRequestModel\",\"namespace\":\"bio.terra.common.iam.avro\",\"fields\":[{\"name\":\"email\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"subject_id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"token\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"to_remove\",\"type\":\"int\",\"default\":99}]}","{\"email\":\"test@example.com\",\"subject_id\":\"Subject\",\"token\":\"0123.456-789AbCd\",\"to_remove\":44}"]],"v2":["java.util.Arrays$ArrayList",["{\"type\":\"record\",\"name\":\"AuthenticatedUserRequestModelV2\",\"namespace\":\"bio.terra.common.iam.avro\",\"fields\":[{\"name\":\"email\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"subject_id\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"token\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"additional_field\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"default\":\"\"}],\"aliases\":[\"AuthenticatedUserRequestModel\"]}","{\"email\":\"test@example.com\",\"subject_id\":\"Subject\",\"token\":\"0123.456-789AbCd\",\"additional_field\":\"foo\"}"]]}]
    logger.info(inMap.toJson());

    // {v1=
    //    [
    //
    // {"type":"record","name":"AuthenticatedUserRequestModel","namespace":"bio.terra.common.iam.avro","fields":[{"name":"email","type":{"type":"string","avro.java.string":"String"}},{"name":"subject_id","type":{"type":"string","avro.java.string":"String"}},{"name":"token","type":{"type":"string","avro.java.string":"String"}},{"name":"to_remove","type":"int","default":99}]},
    //
    // {"email":"test@example.com","subject_id":"Subject","token":"0123.456-789AbCd","to_remove":44}
    //    ],
    //  v2=
    //    [
    //
    // {"type":"record","name":"AuthenticatedUserRequestModelV2","namespace":"bio.terra.common.iam.avro","fields":[{"name":"email","type":{"type":"string","avro.java.string":"String"}},{"name":"subject_id","type":{"type":"string","avro.java.string":"String"}},{"name":"token","type":{"type":"string","avro.java.string":"String"}},{"name":"additional_field","type":{"type":"string","avro.java.string":"String"},"default":""}],"aliases":["AuthenticatedUserRequestModel"]},
    //      {"email":"test@example.com","subjec}...

    logger.info(outMap.toString());

    // Use the V2 parser to extract the V1 serialized message.
    AuthenticatedUserRequestModelV2 message1AsV2 =
        getRecord(
            outMap,
            "v1",
            AuthenticatedUserRequestModelV2.getClassSchema(),
            AuthenticatedUserRequestModelV2.class);
    assertEquals(EMAIL_ADDRESS, message1AsV2.getEmail());
    assertEquals(SUBJECT_ID, message1AsV2.getSubjectId());
    assertEquals(TOKEN, message1AsV2.getToken());
    // The new field gets the default value, the empty string.
    assertEquals("", message1AsV2.getAdditionalField());

    // Use the V1 parser to extract the V2 serialized message.
    AuthenticatedUserRequestModel message2AsV1 =
        getRecord(
            outMap,
            "v2",
            AuthenticatedUserRequestModel.getClassSchema(),
            AuthenticatedUserRequestModel.class);
    assertEquals(EMAIL_ADDRESS, message2AsV1.getEmail());
    assertEquals(SUBJECT_ID, message2AsV1.getSubjectId());
    assertEquals(TOKEN, message2AsV1.getToken());
    // The missing field gets the default value, 99.
    assertEquals(99, message2AsV1.getToRemove());
  }
}
