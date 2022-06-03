package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import com.google.common.annotations.VisibleForTesting;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * This factory is all that should be required to propagate the caller's identity in most cases. In
 * requests where Sam is not otherwise called to check the caller's access or the caller's email or
 * id are required, see {@link SamUserFactory}.
 */
@Component
public class BearerTokenFactory {

  @VisibleForTesting static final String AUTHORIZATION = "Authorization";

  public BearerToken from(HttpServletRequest servletRequest) {
    String authHeader = servletRequest.getHeader(AUTHORIZATION);
    if (authHeader != null) {
      return new BearerToken(BearerTokenParser.parse(authHeader));
    } else {
      throw new UnauthorizedException("Authorization header missing");
    }
  }
}
