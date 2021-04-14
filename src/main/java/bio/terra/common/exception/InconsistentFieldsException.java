package bio.terra.common.exception;

public class InconsistentFieldsException extends BadRequestException {
  public InconsistentFieldsException(String message) {
    super(message);
  }

  public InconsistentFieldsException(String message, Throwable cause) {
    super(message, cause);
  }
}
