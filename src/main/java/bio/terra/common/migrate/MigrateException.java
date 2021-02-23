package bio.terra.common.migrate;

public class MigrateException extends RuntimeException {
  public MigrateException(String message) {
    super(message);
  }

  public MigrateException(String message, Throwable cause) {
    super(message, cause);
  }
}
