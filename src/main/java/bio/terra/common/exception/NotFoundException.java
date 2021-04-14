package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code NOT_FOUND} status in REST response. */
public class NotFoundException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.NOT_FOUND;

  public NotFoundException(String message) {
    super(message, null, thisStatus);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public NotFoundException(Throwable cause) {
    super(cause, thisStatus);
  }

  public NotFoundException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public NotFoundException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
