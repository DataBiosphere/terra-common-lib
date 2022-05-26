package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import org.broadinstitute.dsde.workbench.client.sam.ApiClient;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.api.UsersApi;
import org.broadinstitute.dsde.workbench.client.sam.auth.OAuth;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
public class SamUserAuthenticatedRequestFactoryTest {

  private static final String EMAIL_ADDRESS = "test@example.com";
  private static final String SUBJECT_ID = "Subject";
  private static final TokenAuthenticatedRequest TOKEN = new TokenAuthenticatedRequest.Builder().setToken("0123.456-789AbCd").build();

  @Test
  public void enabledUser() throws ApiException {
    TestFactory factory = new TestFactory();
    when(factory.usersApi.getUserStatusInfo()).thenReturn(new UserStatusInfo().userEmail(EMAIL_ADDRESS).userSubjectId(SUBJECT_ID).enabled(true));

    SamUserAuthenticatedRequest outReq = factory.from(TOKEN, new ApiClient());
    assertEquals(EMAIL_ADDRESS, outReq.getEmail());
    assertEquals(SUBJECT_ID, outReq.getSubjectId());
    assertEquals(TOKEN, outReq.getTokenRequest());
  }

  @Test
  public void disabledUser() throws ApiException {
    TestFactory factory = new TestFactory();
    when(factory.usersApi.getUserStatusInfo()).thenReturn(new UserStatusInfo().userEmail(EMAIL_ADDRESS).userSubjectId(SUBJECT_ID).enabled(false));

    assertThrows(UnauthorizedException.class, () -> factory.from(TOKEN, new ApiClient()));
  }

  @Test
  public void notFoundUser() throws ApiException {
    TestFactory factory = new TestFactory();
    when(factory.usersApi.getUserStatusInfo()).thenThrow(new ApiException(HttpStatus.NOT_FOUND.value(), "not found"));

    assertThrows(UnauthorizedException.class, () -> factory.from(TOKEN, new ApiClient()));
  }

  private static class TestFactory extends SamAuthenticatedUserRequestFactory {
    private UsersApi usersApi = mock(UsersApi.class);

    public TestFactory() {
      super(new TokenAuthenticatedRequestFactory());
    }

    @Override
    protected UsersApi getUsersApi(ApiClient samApiClient) {
      assertTrue(samApiClient.getAuthentications().values().stream().anyMatch(a ->
        a instanceof OAuth && ((OAuth) a).getAccessToken().equals(TOKEN.getToken())
      ));
      return usersApi;
    }
  }
}
