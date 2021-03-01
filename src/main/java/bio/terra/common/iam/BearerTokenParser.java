package bio.terra.common.iam;

import bio.terra.common.exception.UnauthorizedException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utility class for consistently parsing bearer tokens from HTTP Request Authorization headers. */
public final class BearerTokenParser {
  /**
   * Regex to parse bearer token value from authorization header.
   *
   * <pre>
   * See <a href="https://github.com/spring-projects/spring-security/blob/master/oauth2/oauth2-resource-server/src/main/
   * java/org/springframework/security/oauth2/server/resource/web/DefaultBearerTokenResolver.java#L40">here</a> for
   * reference.
   * </pre>
   */
  private static final Pattern authorizationPattern =
      Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$", Pattern.CASE_INSENSITIVE);

  /**
   * Takes the Authorization header passed as a String in {@code authorizationHeader}, parses out
   * the bearer token payload, and returns it as a String.
   *
   * @throws UnauthorizedException if the header is not properly formatted and the bearer token
   *     cannot be parsed.
   */
  public static String parse(String authorizationHeader) {
    Matcher matcher = authorizationPattern.matcher(authorizationHeader);
    if (!matcher.matches()) {
      throw new UnauthorizedException(
          String.format("Invalid Authorization header: '%s'", authorizationHeader));
    }
    return matcher.group("token");
  }
}
