package bio.terra.common.sam;

import static java.time.Instant.now;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpStatus;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SamRetry encapsulates logic needed for retrying Sam API calls. All constants are hard-coded, as
 * Sam failures are generally just short periods of down-time.
 *
 * <p>SamRetry throws either the underlying ApiException from Sam or an InterruptedException.
 */
public class SamRetry {
  private static final Logger logger = LoggerFactory.getLogger(SamRetry.class);

  // The retry function starts with INITIAL_WAIT between retries, and doubles that until it
  // reaches MAXIMUM_WAIT, after which all retries are MAXIMUM_WAIT apart.
  private static final Duration MAXIMUM_WAIT = Duration.ofSeconds(30);
  private static final Duration INITIAL_WAIT = Duration.ofSeconds(10);
  private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(300);
  // Sam calls which timeout will throw ApiExceptions wrapping SocketTimeoutExceptions and will have
  // an errorCode 0. This isn't a real HTTP status code, but we can check for it anyway.
  private static final int TIMEOUT_STATUS_CODE = 0;
  private final Instant operationTimeout;
  // How long to wait between retries.
  private Duration retryDuration;

  SamRetry() {
    this.operationTimeout = now().plus(OPERATION_TIMEOUT);
    this.retryDuration = INITIAL_WAIT;
  }

  protected SamRetry(Duration timeout) {
    this.operationTimeout = now().plus(timeout);
    this.retryDuration = INITIAL_WAIT;
  }

  /**
   * Requests made through the Sam client library sometimes fail with timeouts, generally due to
   * transient network or connection issues. When this happens, the client library will throw an API
   * exceptions with status code 0 wrapping a SocketTimeoutException. These errors should always be
   * retried.
   */
  public static boolean isTimeoutException(ApiException apiException) {
    return apiException.getCode() == TIMEOUT_STATUS_CODE
        && apiException.getCause() instanceof SocketTimeoutException;
  }

  public static <T> T retry(SamFunction<T> function) throws ApiException, InterruptedException {
    SamRetry samRetry = new SamRetry();
    return samRetry.perform(function);
  }

  public static <T> T retry(SamFunction<T> function, Duration timeout)
      throws ApiException, InterruptedException {
    SamRetry samRetry = new SamRetry(timeout);
    return samRetry.perform(function);
  }

  public static void retry(SamVoidFunction function) throws ApiException, InterruptedException {
    SamRetry samRetry = new SamRetry();
    samRetry.performVoid(function);
  }

  public static void retry(SamVoidFunction function, Duration timeout)
      throws ApiException, InterruptedException {
    SamRetry samRetry = new SamRetry(timeout);
    samRetry.performVoid(function);
  }

  private <T> T perform(SamFunction<T> function) throws ApiException, InterruptedException {
    while (true) {
      try {
        return function.apply();
      } catch (ApiException ex) {
        if (isRetryable(ex)) {
          logger.info("SamRetry: caught retry-able exception: ", ex);
          sleepOrTimeoutBeforeRetrying(ex);
        } else {
          throw ex;
        }
      }
    }
  }

  private boolean isRetryable(ApiException apiException) {
    return isTimeoutException(apiException)
        || apiException.getCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || apiException.getCode() == HttpStatus.SC_BAD_GATEWAY
        || apiException.getCode() == HttpStatus.SC_SERVICE_UNAVAILABLE
        || apiException.getCode() == HttpStatus.SC_GATEWAY_TIMEOUT;
  }

  private void performVoid(SamVoidFunction function) throws ApiException, InterruptedException {
    perform(
        () -> {
          function.apply();
          return null;
        });
  }

  /**
   * Given an exception from Sam, either timeout and rethrow the error from Sam or sleep for
   * retryDuration. If the thread times out while sleeping, throw the initial exception.
   *
   * @param previousException The error Sam threw
   * @throws ApiException, InterruptedException
   */
  private void sleepOrTimeoutBeforeRetrying(ApiException previousException)
      throws ApiException, InterruptedException {
    if (operationTimeout.minus(retryDuration).isBefore(now())) {
      logger.error("SamRetry: operation timed out after " + operationTimeout.toString());
      // If we timed out, throw the error from Sam that caused us to need to retry.
      throw previousException;
    }
    logger.info("SamRetry: sleeping " + retryDuration.getSeconds() + " seconds");
    TimeUnit.SECONDS.sleep(retryDuration.getSeconds());
    retryDuration = retryDuration.plus(INITIAL_WAIT);
    if (retryDuration.compareTo(MAXIMUM_WAIT) > 0) {
      retryDuration = MAXIMUM_WAIT;
    }
  }

  @FunctionalInterface
  public interface SamVoidFunction {
    void apply() throws ApiException, InterruptedException;
  }

  @FunctionalInterface
  public interface SamFunction<R> {
    R apply() throws ApiException, InterruptedException;
  }
}
