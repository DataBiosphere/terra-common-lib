package bio.terra.common.sam.exception;

import bio.terra.common.exception.InternalServerErrorException;

/**
 * This exception is thrown when a request fails due to a timeout. Timeouts are generally retried,
 * but this may be thrown if repeated retries fail.
 */
public class SamTimeoutException extends InternalServerErrorException {
  public SamTimeoutException(String message) {
    super(message);
  }

  public SamTimeoutException(String message, Throwable cause) {
    super(message, cause);
  }
}
