package bio.terra.common.sam.exception;

import bio.terra.common.exception.UnauthorizedException;

/**
 * This exception is thrown when we received an {@code UNAUTHORIZED} status in REST response from
 * Sam. Indicates the caller does not have permission to call Sam APIs.
 */
public class SamUnauthorizedException extends UnauthorizedException {
  public SamUnauthorizedException(String message) {
    super(message);
  }

  public SamUnauthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
