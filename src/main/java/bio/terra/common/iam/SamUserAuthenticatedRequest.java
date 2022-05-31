package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/** Class representing the identity of an authenticated user. */
@JsonDeserialize(builder = SamUserAuthenticatedRequest.Builder.class)
public class SamUserAuthenticatedRequest {

  private final String email;
  private final String subjectId;
  private final TokenAuthenticatedRequest tokenRequest;

  private SamUserAuthenticatedRequest(Builder builder) {
    this.email = builder.email;
    this.subjectId = builder.subjectId;
    this.tokenRequest = builder.tokenRequest;
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
  public TokenAuthenticatedRequest getTokenRequest() {
    return tokenRequest;
  }

  @JsonPOJOBuilder(withPrefix = "set")
  public static class Builder {
    private String email;
    private String subjectId;
    private TokenAuthenticatedRequest tokenRequest;

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

    /** Sets the value for {@link #getTokenRequest()}}. */
    public Builder setTokenRequest(TokenAuthenticatedRequest tokenRequest) {
      this.tokenRequest = tokenRequest;
      return this;
    }

    /**
     * Build method returns a constructed, immutable {@link SamUserAuthenticatedRequest} instance.
     *
     * @throws UnauthorizedException if any field are not properly initialized
     */
    public SamUserAuthenticatedRequest build() {

      if (this.email == null) {
        throw new UnauthorizedException("Email is empty.");
      }
      if (this.subjectId == null) {
        throw new UnauthorizedException("Subject is empty.");
      }
      if (this.tokenRequest == null) {
        throw new UnauthorizedException("Token is empty");
      }
      return new SamUserAuthenticatedRequest(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder()
        .setEmail(this.email)
        .setSubjectId(this.subjectId)
        .setTokenRequest(this.tokenRequest);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SamUserAuthenticatedRequest)) {
      return false;
    }
    SamUserAuthenticatedRequest that = (SamUserAuthenticatedRequest) o;
    return Objects.equals(getEmail(), that.getEmail())
        && Objects.equals(getSubjectId(), that.getSubjectId())
        && Objects.equals(getTokenRequest(), that.getTokenRequest());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getEmail())
        .append(getSubjectId())
        .append(getTokenRequest())
        .toHashCode();
  }
}
