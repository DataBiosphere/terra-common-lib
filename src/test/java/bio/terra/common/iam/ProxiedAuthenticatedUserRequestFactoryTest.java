package bio.terra.common.iam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import bio.terra.common.exception.UnauthorizedException;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class ProxiedAuthenticatedUserRequestFactoryTest {

  private static final String EMAIL_ADDRESS = "test@example.com";
  private static final String SUBJECT_ID = "Subject";
  private static final String TOKEN = "0123.456-789AbCd";

  private HttpServletRequest inRequest;

  private static void validate(AuthenticatedUserRequest outReq) {
    assertEquals(EMAIL_ADDRESS, outReq.getEmail());
    assertEquals(SUBJECT_ID, outReq.getSubjectId());
    assertEquals(TOKEN, outReq.getToken());
  }

  @BeforeEach
  public void setUp() {

    // Every test case wants a mock input HttpServletRequest with valid email and subject ID; only
    // token-related headers vary.
    inRequest = mock(HttpServletRequest.class);

    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_CLAIM_EMAIL))
        .thenReturn(EMAIL_ADDRESS);

    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_CLAIM_USER_ID))
        .thenReturn(SUBJECT_ID);
  }

  @Test
  public void accessToken() {
    AuthenticatedUserRequestFactory factory = new ProxiedAuthenticatedUserRequestFactory();
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_ACCESS_TOKEN))
        .thenReturn(TOKEN);
    validate(factory.from(inRequest));
  }

  @Test
  public void bearerToken() {
    final String bearerToken = "Bearer ".concat(TOKEN);
    AuthenticatedUserRequestFactory factory = new ProxiedAuthenticatedUserRequestFactory();
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_ACCESS_TOKEN))
        .thenReturn(null);
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.AUTHORIZATION))
        .thenReturn(bearerToken);
    validate(factory.from(inRequest));
  }

  @Test
  public void nullToken() {
    AuthenticatedUserRequestFactory factory = new ProxiedAuthenticatedUserRequestFactory();
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_ACCESS_TOKEN))
        .thenReturn(null);
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.AUTHORIZATION))
        .thenReturn(null);
    assertThrows(UnauthorizedException.class, () -> factory.from(inRequest));
  }

  @Test
  public void badToken() {
    AuthenticatedUserRequestFactory factory = new ProxiedAuthenticatedUserRequestFactory();
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.OIDC_ACCESS_TOKEN))
        .thenReturn(null);
    when(inRequest.getHeader(ProxiedAuthenticatedUserRequestFactory.AUTHORIZATION))
        .thenReturn("junk");
    assertThrows(UnauthorizedException.class, () -> factory.from(inRequest));
  }
}
