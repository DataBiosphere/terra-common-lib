package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code UNAUTHORIZED} status in REST response. */
public class UnauthorizedException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.UNAUTHORIZED;

  public UnauthorizedException(String message) {
    super(message, null, thisStatus);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public UnauthorizedException(Throwable cause) {
    super(cause, thisStatus);
  }

  public UnauthorizedException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public UnauthorizedException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
