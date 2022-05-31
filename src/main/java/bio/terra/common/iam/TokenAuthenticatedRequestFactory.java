package bio.terra.common.iam;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AuthenticatedUserRequestFactory} when HTTP requests contain a bearer
 * token in the Authorization header.
 *
 * <p>This factory is all that should be required to propagate the caller's identity in most cases.
 * In cases where Sam is not otherwise called to check the caller's access or the caller's email or
 * id are required, seel{@link SamAuthenticatedUserRequestFactory}.
 */
@Component
public class TokenAuthenticatedRequestFactory {

  static final String AUTHORIZATION = "Authorization";

  public TokenAuthenticatedRequest from(HttpServletRequest servletRequest) {
    TokenAuthenticatedRequest.Builder builder = TokenAuthenticatedRequest.builder();

    String authHeader = servletRequest.getHeader(AUTHORIZATION);
    if (authHeader != null) {
      builder.setToken(BearerTokenParser.parse(authHeader));
    }
    return builder.build();
  }
}
