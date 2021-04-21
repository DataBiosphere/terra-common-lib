package bio.terra.common.sam.exception;

import bio.terra.common.exception.ConflictException;

/**
 * This exception is thrown when we received a {@code CONFLICT} status in REST response from Sam.
 * Generally indicates we are trying to create a resource that already exists.
 */
public class SamConflictException extends ConflictException {
  public SamConflictException(String message) {
    super(message);
  }

  public SamConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
