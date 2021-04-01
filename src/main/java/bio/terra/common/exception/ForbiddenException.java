package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code FORBIDDEN} status in REST response. */
public class ForbiddenException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.FORBIDDEN;

  public ForbiddenException(String message) {
    super(message, null, thisStatus);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public ForbiddenException(Throwable cause) {
    super(cause, thisStatus);
  }

  public ForbiddenException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public ForbiddenException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
