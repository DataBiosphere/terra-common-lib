package bio.terra.common.sam.exception;

import bio.terra.common.exception.InternalServerErrorException;

/**
 * This exception is thrown when we received a {@code SERVER_ERROR} status in REST response from
 * Sam.
 */
public class SamInternalServerErrorException extends InternalServerErrorException {
  public SamInternalServerErrorException(String message) {
    super(message);
  }

  public SamInternalServerErrorException(String message, Throwable cause) {
    super(message, cause);
  }
}
