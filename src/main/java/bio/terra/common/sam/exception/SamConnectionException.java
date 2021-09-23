package bio.terra.common.sam.exception;

import bio.terra.common.exception.BadRequestException;

/**
 * This exception is thrown when a request fails due to a connection issue, most often a timeout.
 */
public class SamConnectionException extends BadRequestException {
  public SamConnectionException(String message) {
    super(message);
  }

  public SamConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
