package bio.terra.common.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Class representing the bearer token of the request. */
public class BearerToken {

  private final String token;

  public BearerToken(@JsonProperty("token") String token) {
    this.token = Objects.requireNonNull(token, "token required");
  }

  /** Returns the bearer token given in the Authorization header of the request. */
  public String getToken() {
    return token;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BearerToken other = (BearerToken) o;
    return token.equals(other.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(token);
  }
}
