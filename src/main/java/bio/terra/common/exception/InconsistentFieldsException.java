package bio.terra.common.exception;

/**
 * Exception raised if a service received a request with fields that conflict with each other or
 * with the service's expectations.
 */
public class InconsistentFieldsException extends BadRequestException {
  public InconsistentFieldsException(String message) {
    super(message);
  }

  public InconsistentFieldsException(String message, Throwable cause) {
    super(message, cause);
  }
}
