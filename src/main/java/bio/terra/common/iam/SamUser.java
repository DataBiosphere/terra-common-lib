package bio.terra.common.iam;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Class representing the identity of an authenticated user. */
public class SamUser {

  private final String email;
  private final String subjectId;
  private final BearerToken bearerToken;

  public SamUser(
      @JsonProperty("email") String email,
      @JsonProperty("subjectId") String subjectId,
      @JsonProperty("bearerToken") BearerToken bearerToken) {
    this.email = Objects.requireNonNull(email, "email required");
    this.subjectId = Objects.requireNonNull(subjectId, "subjectId required");
    this.bearerToken = Objects.requireNonNull(bearerToken, "bearerToken required");
  }

  /** Returns the email address of the authenticated user */
  public String getEmail() {
    return email;
  }

  /** Returns a unique, stable identifier for the authenticated user */
  public String getSubjectId() {
    return subjectId;
  }

  /** Returns the BearerToken associated with this request */
  public BearerToken getBearerToken() {
    return bearerToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SamUser other = (SamUser) o;
    return email.equals(other.email)
        && subjectId.equals(other.subjectId)
        && bearerToken.equals(other.bearerToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, subjectId, bearerToken);
  }
}
