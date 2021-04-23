package bio.terra.common.sam.exception;

import bio.terra.common.exception.ForbiddenException;

/**
 * This exception is thrown when we received a {@code FORBIDDEN} status in REST response from Sam.
 * Indicates the user does not have permission to perform the given action.
 */
public class SamForbiddenException extends ForbiddenException {
  public SamForbiddenException(String message) {
    super(message);
  }

  public SamForbiddenException(String message, Throwable cause) {
    super(message, cause);
  }
}
