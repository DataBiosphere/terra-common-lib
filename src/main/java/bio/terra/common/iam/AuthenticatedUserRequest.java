package bio.terra.common.iam;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.avro.AuthenticatedUserRequestModel;
import bio.terra.stairway.FlightMap;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** Class representing the identity of an authenticated user. */
@JsonDeserialize(builder = AuthenticatedUserRequest.Builder.class)
public class AuthenticatedUserRequest {

  private final String email;
  private final String subjectId;
  private final String token;

  private AuthenticatedUserRequest(Builder builder) {
    this.email = builder.email;
    this.subjectId = builder.subjectId;
    this.token = builder.token;
  }

  /**
   * Returns the email address of the authenticated user, corresponding to the OIDC 'email' claim.
   */
  public String getEmail() {
    return email;
  }

  /**
   * Returns a unique, stable identifier for the authenticated user, corresponding to the OIDC 'sub'
   * claim. This may not be set and thus may return a null reference.
   */
  public String getSubjectId() {
    return subjectId;
  }

  /** Returns a JSON Web Token (JWT) possessed by the authenticated user. */
  public String getToken() {
    return token;
  }

  public FlightMap putIn(FlightMap flightMap, String key) {
    return putRecord(
        flightMap,
        key,
        AuthenticatedUserRequestModel.getClassSchema(),
        toModel(),
        AuthenticatedUserRequestModel.class);
  }

  public static AuthenticatedUserRequest getFrom(FlightMap flightMap, String key) {
    AuthenticatedUserRequestModel model =
        getRecord(
            flightMap,
            key,
            AuthenticatedUserRequestModel.getClassSchema(),
            AuthenticatedUserRequestModel.class);
    return fromModel(model);
  }

  /**
   * This could be put on FlightMap as a part of Stairway. Broken out to a separate function here to
   * illustrate.
   */
  @VisibleForTesting
  static <T> FlightMap putRecord(
      FlightMap flightMap, String key, Schema schema, T record, Class<T> type) {
    try {

      // Encode the object with the "writer" schema into the byte stream.
      OutputStream stream = new ByteArrayOutputStream();
      Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, stream);
      SpecificDatumWriter<T> writer = new SpecificDatumWriter<T>(schema);
      writer.write(record, jsonEncoder);
      jsonEncoder.flush();

      // Avro requires "writer" schema to be stored in order be able to evolve schema.  Normally
      // schema versions are stored in a "schema repository" and a "schema fingerprint" is stored
      // with the data instead of the actual schema.  In this case we'll store the schema as a
      // string along with the data itself in an array.
      List<String> value = Arrays.asList(schema.toString(), stream.toString());
      flightMap.put(key, value);

      return flightMap;
    } catch (final IOException ex) {
      throw new InternalServerErrorException(
          String.format("Unable to write message for key %s", key));
    }
  }

  /**
   * This could be put on FlightMap as a part of Stairway. Broken out to a separate function here to
   * illustrate.
   */
  @VisibleForTesting
  static <T> T getRecord(FlightMap flightMap, String key, Schema readerSchema, Class<T> type) {
    try {
      // Schema evolution requires the "reader" and "writer" schemas in order to resolve
      // differences.  Parse the "writer" schema from the stored data, the "reader" schema is
      // passed.
      List<String> value = flightMap.get(key, ArrayList.class);
      Schema.Parser parser = new Schema.Parser();
      Schema writerSchema = parser.parse(value.get(0));

      // Pass both schemas and decode the data.
      DatumReader<T> reader = new SpecificDatumReader<T>(writerSchema, readerSchema);
      Decoder decoder = DecoderFactory.get().jsonDecoder(writerSchema, value.get(1));
      return reader.read(null, decoder);
    } catch (final IOException ex) {
      throw new InternalServerErrorException(
          String.format("Unable to parse message at key %s", key));
    }
  }

  private AuthenticatedUserRequestModel toModel() {
    return AuthenticatedUserRequestModel.newBuilder()
        .setEmail(email)
        .setSubjectId(subjectId)
        .setToken(token)
        .build();
  }

  private static AuthenticatedUserRequest fromModel(AuthenticatedUserRequestModel model) {
    return builder()
        .setEmail(model.getEmail())
        .setSubjectId(model.getSubjectId())
        .setToken(model.getToken())
        .build();
  }

  @JsonPOJOBuilder(withPrefix = "set")
  public static class Builder {
    private String email;
    private String subjectId;
    private String token;

    /** Sets the value for {@link #getEmail()}. */
    public Builder setEmail(String email) {
      this.email = email;
      return this;
    }

    /** Sets the value for {@link #getSubjectId()}}. */
    public Builder setSubjectId(String subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    /** Sets the value for {@link #getToken()}}. */
    public Builder setToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * Build method returns a constructed, immutable {@link AuthenticatedUserRequest} instance.
     *
     * @throws UnauthorizedException if any field are not properly initialized
     */
    public AuthenticatedUserRequest build() {

      if (this.email == null) {
        throw new IllegalStateException("Email is empty.");
      }
      if (this.subjectId == null) {
        throw new IllegalStateException("Subject is empty.");
      }
      if (this.token == null) {
        throw new IllegalStateException("Token is empty");
      }
      return new AuthenticatedUserRequest(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder().setEmail(this.email).setSubjectId(this.subjectId).setToken(this.token);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuthenticatedUserRequest)) {
      return false;
    }
    AuthenticatedUserRequest that = (AuthenticatedUserRequest) o;
    return Objects.equals(getEmail(), that.getEmail())
        && Objects.equals(getSubjectId(), that.getSubjectId())
        && Objects.equals(getToken(), that.getToken());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getEmail())
        .append(getSubjectId())
        .append(getToken())
        .toHashCode();
  }
}
