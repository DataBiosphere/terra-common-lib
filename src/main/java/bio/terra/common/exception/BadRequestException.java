package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code BAD_REQUEST} status in REST response. */
public class BadRequestException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.BAD_REQUEST;

  public BadRequestException(String message) {
    super(message, null, thisStatus);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public BadRequestException(Throwable cause) {
    super(null, cause, null, thisStatus);
  }

  public BadRequestException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public BadRequestException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
