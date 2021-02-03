package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code INTERNAL_SERVER_ERROR} status in REST response. */
public class InternalServerErrorException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.INTERNAL_SERVER_ERROR;

  public InternalServerErrorException(String message) {
    super(message, null, thisStatus);
  }

  public InternalServerErrorException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public InternalServerErrorException(Throwable cause) {
    super(cause, thisStatus);
  }

  public InternalServerErrorException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public InternalServerErrorException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
