package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.iam.thrift.AuthenticatedUserRequestModel;
import bio.terra.stairway.FlightMap;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

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
    return putMessage(flightMap, key, toModel());
  }

  public static AuthenticatedUserRequest getFrom(FlightMap flightMap, String key) {
    AuthenticatedUserRequestModel model =
        getMessage(flightMap, key, new AuthenticatedUserRequestModel());
    return AuthenticatedUserRequest.builder()
        .setEmail(model.getEmail())
        .setSubjectId(model.getSubjectId())
        .setToken(model.getToken())
        .build();
  }

  @VisibleForTesting
  static <T extends TBase<T, F>, F extends TFieldIdEnum> FlightMap putMessage(
      FlightMap flightMap, String key, TBase<T, F> message) {
    try {
      TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());
      byte[] serialized = serializer.serialize(message);
      flightMap.put(key, new String(serialized, StandardCharsets.UTF_8));
      return flightMap;
    } catch (final TException ex) {
      throw new RuntimeException("Serialization failed.", ex);
    }
  }

  @VisibleForTesting
  static <T extends TBase<T, F>, F extends TFieldIdEnum> T getMessage(
      FlightMap flightMap, String key, T message) {
    try {
      byte[] serialized = flightMap.get(key, String.class).getBytes();
      TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());
      deserializer.deserialize(message, serialized);
      return message;
    } catch (final TException ex) {
      throw new RuntimeException("Deserialization failed.", ex);
    }
  }

  private AuthenticatedUserRequestModel toModel() {
    return new AuthenticatedUserRequestModel()
        .setEmail(getEmail())
        .setSubjectId(getSubjectId())
        .setToken(getToken());
  }

  private AuthenticatedUserRequest fromModel(AuthenticatedUserRequestModel model) {
    return AuthenticatedUserRequest.builder()
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
