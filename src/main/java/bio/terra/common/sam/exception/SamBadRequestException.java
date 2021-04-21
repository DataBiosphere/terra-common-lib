package bio.terra.common.sam.exception;

import bio.terra.common.exception.BadRequestException;

/**
 * This exception is thrown when we received a {@code BAD_REQUEST} status in REST response from Sam.
 * Indicates a malformed request.
 */
public class SamBadRequestException extends BadRequestException {
  public SamBadRequestException(String message) {
    super(message);
  }

  public SamBadRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
