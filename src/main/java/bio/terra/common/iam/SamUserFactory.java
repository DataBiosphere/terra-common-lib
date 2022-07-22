package bio.terra.common.iam;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.exception.UnauthorizedException;
import bio.terra.common.tracing.OkHttpClientTracingInterceptor;
import com.google.common.annotations.VisibleForTesting;
import javax.servlet.http.HttpServletRequest;

import io.opencensus.trace.Tracing;
import okhttp3.OkHttpClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Used to verify that a user is enabled and to get accurate email and id for the user from Sam.
 * Email and id embedded in the request's authentication token are not always accurate (in the case
 * of pet identities) and should not be used. Please do not use by default on every request, instead
 * use {@link BearerTokenFactory}. Use this factory only when user id and email are actually
 * required or to verify a user is enabled. These cases should be in the minority because every /api
 * call to Sam checks if the user is enabled and most service api calls should check a Sam
 * permission. Emails should be seldom used within the system and should not be stored as they may
 * change.
 */
@Component
public class SamUserFactory {
  final BearerTokenFactory bearerTokenFactory;

  @Autowired
  public SamUserFactory(BearerTokenFactory bearerTokenFactory) {
    this.bearerTokenFactory = bearerTokenFactory;
  }

  public SamUser from(HttpServletRequest servletRequest, String samBasePath) {
    return from(bearerTokenFactory.from(servletRequest), samBasePath);
  }

  public SamUser from(BearerToken bearerToken, String samBasePath) {
    UsersApi usersApi = createUsersApi(bearerToken, samBasePath);

    try {
      UserStatusInfo userStatusInfo = usersApi.getUserStatusInfo();
      if (!userStatusInfo.getEnabled()) {
        throw new UnauthorizedException("User is disabled, please contact Terra support");
      }
      return new SamUser(
          userStatusInfo.getUserEmail(), userStatusInfo.getUserSubjectId(), bearerToken);
    } catch (final NullPointerException e) {
      throw new UnauthorizedException(e.getMessage(), e);
    } catch (final ApiException e) {
      if (e.getCode() == HttpStatus.NOT_FOUND.value()) {
        throw new UnauthorizedException("User not found", e);
      } else {
        throw new InternalServerErrorException(e);
      }
    }
  }

  @VisibleForTesting
  UsersApi createUsersApi(BearerToken bearerToken, String samBasePath) {
    ApiClient samApiClient = new ApiClient();
    OkHttpClient okHttpClient = samApiClient
        .getHttpClient()
        .newBuilder()
        .addInterceptor(new OkHttpClientTracingInterceptor(Tracing.getTracer()))
        .build();

    samApiClient.setHttpClient(okHttpClient);
    samApiClient.setBasePath(samBasePath);
    samApiClient.setAccessToken(bearerToken.getToken());
    return new UsersApi(samApiClient);
  }
}
