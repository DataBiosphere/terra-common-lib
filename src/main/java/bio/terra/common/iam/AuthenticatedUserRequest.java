package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import bio.terra.stairway.FlightMap;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import java.util.Objects;
import java.util.function.Function;
import org.apache.commons.lang.builder.HashCodeBuilder;

/** Class representing the identity of an authenticated user. */
@JsonDeserialize(builder = AuthenticatedUserRequest.Builder.class)
public class AuthenticatedUserRequest {

  private final String email;
  private final String subjectId;
  private final String token;

  private static final JsonFactory factory = new JsonFactory();
  private static final JsonMapper mapper = new JsonMapper();

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
    return putJson(flightMap, key, toJson());
  }

  public static AuthenticatedUserRequest getFrom(FlightMap flightMap, String key) {
    return getJson(flightMap, key, AuthenticatedUserRequest::fromJson);
  }

  @VisibleForTesting
  static FlightMap putJson(FlightMap flightMap, String key, JsonNode root) {
    flightMap.put(key, root.toString());
    return flightMap;
  }

  @VisibleForTesting
  static <T> T getJson(FlightMap flightMap, String key, Function<JsonNode, T> getter) {
    try {
      JsonNode node = mapper.readTree(flightMap.get(key, String.class));
      return getter.apply(node);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Error deserializing.");
    }
  }

  @VisibleForTesting static final int DefaultToRemove = 99;
  @VisibleForTesting int toRemove = DefaultToRemove;

  @VisibleForTesting static final String DefaultAdditionalField = "";
  @VisibleForTesting String additionalField = DefaultAdditionalField;

  private JsonNode toJson() {
    return toJson(this);
  }

  @VisibleForTesting
  static JsonNode toJson(AuthenticatedUserRequest request) {
    ObjectNode node = mapper.createObjectNode();
    node.put("email", request.getEmail());
    node.put("subject_id", request.getSubjectId());
    node.put("token", request.getToken());
    node.put("to_remove", request.toRemove);
    return node;
  }

  @VisibleForTesting
  static AuthenticatedUserRequest fromJson(JsonNode root) {

    AuthenticatedUserRequest request =
        AuthenticatedUserRequest.builder()
            .setEmail(root.get("email").asText())
            .setSubjectId(root.get("subject_id").asText())
            .setToken(root.get("token").asText())
            .build();

    request.toRemove =
        root.has("to_remove") ? root.get("to_remove").asInt(DefaultToRemove) : DefaultToRemove;

    return request;
  }

  @VisibleForTesting
  static JsonNode toJsonV2(AuthenticatedUserRequest request) {
    ObjectNode node = mapper.createObjectNode();
    node.put("email", request.getEmail());
    node.put("subject_id", request.getSubjectId());
    node.put("token", request.getToken());
    node.put("additional_field", request.additionalField);
    return node;
  }

  @VisibleForTesting
  static AuthenticatedUserRequest fromJsonV2(JsonNode root) {
    AuthenticatedUserRequest request =
        AuthenticatedUserRequest.builder()
            .setEmail(root.get("email").asText())
            .setSubjectId(root.get("subject_id").asText())
            .setToken(root.get("token").asText())
            .build();

    request.additionalField =
        root.has("additional_field")
            ? root.get("additional_field").asText(DefaultAdditionalField)
            : DefaultAdditionalField;

    return request;
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
