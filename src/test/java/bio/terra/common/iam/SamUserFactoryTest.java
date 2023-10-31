package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import bio.terra.common.exception.UnauthorizedException;
import java.util.Optional;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@Tag("unit")
public class SamUserFactoryTest {

  private static final SamUser SAM_USER =
      new SamUser("test@example.com", "Subject", new BearerToken("0123.456-789AbCd"));
  private static final String SAM_BASE_PATH = "not_real";

  @Test
  public void enabledUser() throws ApiException {
    SamUserFactory factory = spy(new SamUserFactory(new BearerTokenFactory(), Optional.empty()));
    UsersApi usersApi = mock(UsersApi.class);
    when(factory.createUsersApi(SAM_USER.getBearerToken(), SAM_BASE_PATH)).thenReturn(usersApi);
    when(usersApi.getUserStatusInfo())
        .thenReturn(
            new UserStatusInfo()
                .userEmail(SAM_USER.getEmail())
                .userSubjectId(SAM_USER.getSubjectId())
                .enabled(true));

    SamUser outReq = factory.from(SAM_USER.getBearerToken(), SAM_BASE_PATH);
    assertEquals(SAM_USER, outReq);
  }

  @Test
  public void disabledUser() throws ApiException {
    SamUserFactory factory = spy(new SamUserFactory(new BearerTokenFactory(), Optional.empty()));
    UsersApi usersApi = mock(UsersApi.class);
    when(factory.createUsersApi(SAM_USER.getBearerToken(), SAM_BASE_PATH)).thenReturn(usersApi);
    when(usersApi.getUserStatusInfo())
        .thenReturn(
            new UserStatusInfo()
                .userEmail(SAM_USER.getEmail())
                .userSubjectId(SAM_USER.getSubjectId())
                .enabled(false));

    assertThrows(
        UnauthorizedException.class, () -> factory.from(SAM_USER.getBearerToken(), SAM_BASE_PATH));
  }

  @Test
  public void notFoundUser() throws ApiException {
    SamUserFactory factory = spy(new SamUserFactory(new BearerTokenFactory(), Optional.empty()));
    UsersApi usersApi = mock(UsersApi.class);
    when(factory.createUsersApi(SAM_USER.getBearerToken(), SAM_BASE_PATH)).thenReturn(usersApi);
    when(usersApi.getUserStatusInfo())
        .thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "not found"));

    assertThrows(
        UnauthorizedException.class, () -> factory.from(SAM_USER.getBearerToken(), SAM_BASE_PATH));
  }
}
