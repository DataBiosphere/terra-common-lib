package bio.terra.common.exception;

import java.util.List;
import org.springframework.http.HttpStatus;

/** This exception returns {@code SERVICE_UNAVAILABLE} status in REST response. */
public class ServiceUnavailableException extends ErrorReportException {
  private static final HttpStatus thisStatus = HttpStatus.SERVICE_UNAVAILABLE;

  public ServiceUnavailableException(String message) {
    super(message, null, thisStatus);
  }

  public ServiceUnavailableException(String message, Throwable cause) {
    super(message, cause, null, thisStatus);
  }

  public ServiceUnavailableException(Throwable cause) {
    super(cause, thisStatus);
  }

  public ServiceUnavailableException(String message, List<String> causes) {
    super(message, causes, thisStatus);
  }

  public ServiceUnavailableException(String message, Throwable cause, List<String> causes) {
    super(message, cause, causes, thisStatus);
  }
}
