package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code CONFLICT} status in REST response. */
public class ConflictException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.CONFLICT;

  public ConflictException(String message) {
    super(message, null, thisStatus);
  }

  public ConflictException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public ConflictException(Throwable cause) {
    super(cause, thisStatus);
  }

  public ConflictException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public ConflictException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
