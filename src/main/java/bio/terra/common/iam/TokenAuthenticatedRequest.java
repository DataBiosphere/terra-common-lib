package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;

/** Class representing the bearer token of the request. */
@JsonDeserialize(builder = TokenAuthenticatedRequest.Builder.class)
public class TokenAuthenticatedRequest {

  private final String token;

  private TokenAuthenticatedRequest(Builder builder) {
    this.token = builder.token;
  }

  /** Returns the bearer token given in the Authorization header of the request. */
  public String getToken() {
    return token;
  }

  @JsonPOJOBuilder(withPrefix = "set")
  public static class Builder {
    private String token;

    /** Sets the value for {@link #getToken()}}. */
    public Builder setToken(String token) {
      this.token = token;
      return this;
    }

    /**
     * Build method returns a constructed, immutable {@link TokenAuthenticatedRequest} instance.
     *
     * @throws UnauthorizedException if any field are not properly initialized
     */
    public TokenAuthenticatedRequest build() {

      if (this.token == null) {
        throw new UnauthorizedException("Token is empty");
      }
      return new TokenAuthenticatedRequest(this);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return builder().setToken(this.token);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TokenAuthenticatedRequest)) {
      return false;
    }
    TokenAuthenticatedRequest that = (TokenAuthenticatedRequest) o;
    return Objects.equals(getToken(), that.getToken());
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder()
        .append(getToken())
        .toHashCode();
  }
}
