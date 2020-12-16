package bio.terra.common.migirate;

public class MigrateException extends RuntimeException {
  public MigrateException(String message) {
    super(message);
  }

  public MigrateException(String message, Throwable cause) {
    super(message, cause);
  }
}
