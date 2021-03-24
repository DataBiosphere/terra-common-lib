package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Implementation of {link AuthenticatedUserRequestFactory} when HTTP requests enter from Apache
 * Proxy. In this scenario, service is deployed behind Apache Proxy, and Apache proxy clears out any
 * inbound values for these headers, so they are guaranteed to contain valid auth information.
 */
@Component
public class ProxiedAuthenticatedUserRequestFactory implements AuthenticatedUserRequestFactory {

  static final String OIDC_ACCESS_TOKEN = "OIDC_ACCESS_token";
  static final String AUTHORIZATION = "Authorization";
  static final String OIDC_CLAIM_EMAIL = "OIDC_CLAIM_email";
  static final String OIDC_CLAIM_USER_ID = "OIDC_CLAIM_user_id";

  @Override
  public AuthenticatedUserRequest from(HttpServletRequest servletRequest) {
    AuthenticatedUserRequest.Builder builder =
        AuthenticatedUserRequest.builder()
            .setEmail(servletRequest.getHeader(OIDC_CLAIM_EMAIL))
            .setSubjectId(servletRequest.getHeader(OIDC_CLAIM_USER_ID));

    String token = servletRequest.getHeader(OIDC_ACCESS_TOKEN);

    if (token != null) {
      builder.setToken(token);
    } else {
      String authHeader = servletRequest.getHeader(AUTHORIZATION);
      if (authHeader != null) {
        builder.setToken(BearerTokenParser.parse(authHeader));
      }
    }
    AuthenticatedUserRequest request;
    try {
      request = builder.build();
    } catch (final IllegalStateException e) {
      throw new UnauthorizedException(
          String.format("Building AuthenticatedUserRequest failed with error: %s", e.getMessage()));
    }
    return request;
  }
}
