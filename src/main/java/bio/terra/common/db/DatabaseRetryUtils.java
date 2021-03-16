package bio.terra.common.db;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;

/** Utilities to execute database operations with retry support. */
public final class DatabaseRetryUtils {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryUtils.class);

  private DatabaseRetryUtils() {}

  /**
   * Executes a database operation and retries if retryable.
   *
   * @param execute database operation to execute
   * @param retrySleep fixed retry sleep interval
   * @param maxNumAttempts maximum retries
   * @param <T> database operation class
   * @return database operation class
   * @throws InterruptedException on thread interruption
   */
  public static <T> T executeAndRetry(
      DatabaseOperation<T> execute, Duration retrySleep, int maxNumAttempts)
      throws InterruptedException {
    int numAttempts = 1;
    while (numAttempts <= maxNumAttempts) {
      try {
        return execute.execute();
      } catch (DataAccessException e) {
        if (!shouldRetryQuery(e)) {
          throw e;
        }
        logger.info("Caught exception, retrying DB operation. Attempts so far: {}", numAttempts, e);
      }
      ++numAttempts;
      TimeUnit.MILLISECONDS.sleep(retrySleep.toMillis());
    }
    throw new InterruptedException("Exceeds maximum number of retries.");
  }

  /**
   * Tests an exception to see if it is retryable.
   *
   * @param dataAccessException execption to test
   * @return {@code true} if that is retryable {@link DataAccessException}.
   */
  public static boolean shouldRetryQuery(DataAccessException dataAccessException) {
    return ExceptionUtils.hasCause(dataAccessException, RecoverableDataAccessException.class)
        || ExceptionUtils.hasCause(dataAccessException, TransientDataAccessException.class);
  }

  /** How to execute this database operation. */
  @FunctionalInterface
  public interface DatabaseOperation<T> {
    T execute();
  }
}
