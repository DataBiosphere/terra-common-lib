package bio.terra.common.sam.exception;

import bio.terra.common.exception.InternalServerErrorException;

/**
 * This exception is thrown when an Interrupted Exception is raised while Sam is retrying. This
 * should be used in contexts outside of Stairway. In Stairway, the InterruptedException itself
 * should be raised.
 */
public class SamInterruptedException extends InternalServerErrorException {

  public SamInterruptedException(String message) {
    super(message);
  }

  public SamInterruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}
