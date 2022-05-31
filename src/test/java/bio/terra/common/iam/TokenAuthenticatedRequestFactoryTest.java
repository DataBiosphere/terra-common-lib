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
public class TokenAuthenticatedRequestFactoryTest {

  private static final String TOKEN = "0123.456-789AbCd";

  private HttpServletRequest inRequest;

  private static void validate(TokenAuthenticatedRequest outReq) {
    assertEquals(TOKEN, outReq.getToken());
  }

  @BeforeEach
  public void setUp() {

    // Every test case wants a mock input HttpServletRequest with valid email and subject ID; only
    // token-related headers vary.
    inRequest = mock(HttpServletRequest.class);
  }

  @Test
  public void bearerToken() {
    final String bearerToken = "Bearer ".concat(TOKEN);
    TokenAuthenticatedRequestFactory factory = new TokenAuthenticatedRequestFactory();
    when(inRequest.getHeader(TokenAuthenticatedRequestFactory.AUTHORIZATION))
        .thenReturn(bearerToken);
    validate(factory.from(inRequest));
  }

  @Test
  public void nullToken() {
    TokenAuthenticatedRequestFactory factory = new TokenAuthenticatedRequestFactory();
    when(inRequest.getHeader(TokenAuthenticatedRequestFactory.AUTHORIZATION)).thenReturn(null);
    assertThrows(UnauthorizedException.class, () -> factory.from(inRequest));
  }

  @Test
  public void badToken() {
    TokenAuthenticatedRequestFactory factory = new TokenAuthenticatedRequestFactory();
    when(inRequest.getHeader(TokenAuthenticatedRequestFactory.AUTHORIZATION)).thenReturn("junk");
    assertThrows(UnauthorizedException.class, () -> factory.from(inRequest));
  }
}
