package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import javax.servlet.http.HttpServletRequest;

/**
 * An interface for extracting {@link AuthenticatedUserRequest} from an {@link HttpServletRequest}.
 *
 * @deprecated use {@link BearerTokenFactory} instead
 */
@Deprecated
public interface AuthenticatedUserRequestFactory {

  /**
   * Returns an {@link AuthenticatedUserRequest} built from the headers of the {@link
   * HttpServletRequest} instance passed in {@code servletRequest}.
   *
   * @throws UnauthorizedException if any of the fields required to build an {@link
   *     AuthenticatedUserRequest} (email, subject ID, and JWT token) cannot be extracted from the
   *     passed headers.
   */
  AuthenticatedUserRequest from(HttpServletRequest servletRequest);
}
