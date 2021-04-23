package bio.terra.common.sam.exception;

import bio.terra.common.exception.NotFoundException;

/**
 * This exception is thrown when we received a {@code NOT_FOUND} status in REST response from Sam.
 * Generally indicates the given resource does not exist.
 */
public class SamNotFoundException extends NotFoundException {
  public SamNotFoundException(String message) {
    super(message);
  }

  public SamNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
