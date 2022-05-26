package bio.terra.common.iam;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.exception.UnauthorizedException;
import com.google.common.annotations.VisibleForTesting;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * Used verify a user is enabled and to get accurate email and id for the user from Sam.
 * Email and id embedded in the request's authentication token are not always accurate (in the case of pet identities) and should not be used.
 * Please do not use by default on every request, instead use {@link TokenAuthenticatedRequestFactory}
 * Use this factory only when user id and email are actually required or to verify a user is enabled.
 * These cases should be in the minority because every /api call to Sam checks if the user is enabled and most
 * service api calls should check a Sam permission. Emails should be seldom used within the system and should not
 * be stored as they may change.
 */
@Component
public class SamAuthenticatedUserRequestFactory {
  final TokenAuthenticatedRequestFactory tokenAuthenticatedRequestFactory;

  @Autowired
  public SamAuthenticatedUserRequestFactory(TokenAuthenticatedRequestFactory tokenAuthenticatedRequestFactory) {
    this.tokenAuthenticatedRequestFactory = tokenAuthenticatedRequestFactory;
  }

  public SamUserAuthenticatedRequest from(HttpServletRequest servletRequest, ApiClient samApiClient) {
    return from(tokenAuthenticatedRequestFactory.from(servletRequest), samApiClient);
  }

  public SamUserAuthenticatedRequest from(TokenAuthenticatedRequest tokenAuthenticatedRequest, ApiClient samApiClient) {
    SamUserAuthenticatedRequest.Builder builder = SamUserAuthenticatedRequest.builder().setTokenRequest(tokenAuthenticatedRequest);

    samApiClient.setAccessToken(tokenAuthenticatedRequest.getToken());
    UsersApi usersApi = getUsersApi(samApiClient);

    try {
      UserStatusInfo userStatusInfo = usersApi.getUserStatusInfo();
      if (!userStatusInfo.getEnabled()) {
        throw new UnauthorizedException("User is disabled, please contact Terra support");
      }
      return builder.setEmail(userStatusInfo.getUserEmail()).setSubjectId(userStatusInfo.getUserSubjectId()).build();
    } catch (final IllegalStateException e) {
      throw new UnauthorizedException(
          String.format("Building AuthenticatedUserRequest failed with error: %s", e.getMessage()), e);
    } catch (final ApiException e) {
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        throw new UnauthorizedException("User not found", e);
      } else {
        throw new InternalServerErrorException(e);
      }
    }
  }

  @VisibleForTesting
  protected UsersApi getUsersApi(ApiClient samApiClient) {
    return new UsersApi(samApiClient);
  }
}
