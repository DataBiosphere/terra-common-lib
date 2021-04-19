package bio.terra.common.exception;

public class MissingRequiredFieldException extends BadRequestException {
  public MissingRequiredFieldException(String message) {
    super(message);
  }

  public MissingRequiredFieldException(String message, Throwable cause) {
    super(message, cause);
  }
}
