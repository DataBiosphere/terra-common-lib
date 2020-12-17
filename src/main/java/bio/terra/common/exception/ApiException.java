package bio.terra.common.exception;

/**
 * API exception caused by internal server error. Using this will get {@code INTERNAL_SERVER_ERROR}
 * inherited from its super class.
 */
public class ApiException extends InternalServerErrorException {
  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }
}
