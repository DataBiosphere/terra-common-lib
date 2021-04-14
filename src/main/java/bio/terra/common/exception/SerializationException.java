package bio.terra.common.exception;

/**
 * Exception thrown for serialization errors. Generally, this indicates that a code change has
 * broken serialization or deserialization.
 */
public class SerializationException extends InternalServerErrorException {
  public SerializationException(String message) {
    super(message);
  }

  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
