package bio.terra.common.retry;

import static java.time.Instant.now;

import bio.terra.common.exception.BadRequestException;
import bio.terra.common.exception.ConflictException;
import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.exception.ForbiddenException;
import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.exception.NotFoundException;
import bio.terra.common.exception.UnauthorizedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * SamRetry encapsulates logic needed for retrying Sam API calls. All constants are hard-coded, as
 * Sam failures are generally just short periods of down-time.
 */
public class SamRetry {
  private static Logger logger = LoggerFactory.getLogger(SamRetry.class);

  // The retry function starts with INITIAL_WAIT between retries, and doubles that until it
  // reaches MAXIMUM_WAIT, after which all retries are MAXIMUM_WAIT apart.
  private static final Duration MAXIMUM_WAIT = Duration.ofSeconds(30);
  private static final Duration INITIAL_WAIT = Duration.ofSeconds(10);
  private static final Duration OPERATION_TIMEOUT = Duration.ofSeconds(300);
  private final Instant operationTimeout;

  // How long to wait between retries.
  private Duration retryDuration;

  private static final ObjectMapper objectMapper = new ObjectMapper();

  SamRetry() {
    this.operationTimeout = now().plus(OPERATION_TIMEOUT);
    this.retryDuration = INITIAL_WAIT;
  }

  @VisibleForTesting
  SamRetry(Duration timeout) {
    this.operationTimeout = now().plus(timeout);
    this.retryDuration = INITIAL_WAIT;
  }

  @FunctionalInterface
  public interface SamVoidFunction {
    void apply() throws ApiException, InterruptedException;
  }

  @FunctionalInterface
  public interface SamFunction<R> {
    R apply() throws ApiException, InterruptedException;
  }

  public static <T> T retry(SamFunction<T> function) throws InterruptedException {
    SamRetry samRetry = new SamRetry();
    return samRetry.perform(function);
  }

  // Version of the retry function for testing so that we can override timeout
  @VisibleForTesting
  public static <T> T retry(SamFunction<T> function, Duration timeout) throws InterruptedException {
    SamRetry samRetry = new SamRetry(timeout);
    return samRetry.perform(function);
  }

  public static void retry(SamVoidFunction function) throws InterruptedException {
    SamRetry samRetry = new SamRetry();
    samRetry.performVoid(function);
  }

  // Version of the retry function for testing so that we can override timeout
  @VisibleForTesting
  public static void retry(SamVoidFunction function, Duration timeout) throws InterruptedException {
    SamRetry samRetry = new SamRetry(timeout);
    samRetry.performVoid(function);
  }

  private <T> T perform(SamFunction<T> function) throws InterruptedException {
    while (true) {
      try {
        return function.apply();
      } catch (ApiException ex) {
        ErrorReportException errorReportException = convertSAMExToCommonEx(ex);
        if (isRetryable(errorReportException)) {
          logger.info("SamRetry: caught retry-able exception: ", ex);
          sleepOrTimeoutBeforeRetrying(errorReportException);
          continue;
        } else {
          throw errorReportException;
        }
      } catch (Exception ex) {
        throw new InternalServerErrorException("Unexpected exception type: " + ex.toString(), ex);
      }
    }
  }

  private boolean isRetryable(ErrorReportException errorReportException) {
    return (errorReportException.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private void performVoid(SamVoidFunction function) throws InterruptedException {
    perform(
        () -> {
          function.apply();
          return null;
        });
  }

  private void sleepOrTimeoutBeforeRetrying(ErrorReportException errorReportException)
      throws InterruptedException {
    // if our operation timeout will happen before we wake up from our next retry
    // sleep, then we give up and re-throw.
    if (operationTimeout.minus(retryDuration).isBefore(now())) {
      logger.error("SamRetry: operation timed out after " + operationTimeout.toString());
      throw errorReportException;
    }
    logger.info("SamRetry: sleeping " + retryDuration.getSeconds() + " seconds");
    TimeUnit.SECONDS.sleep(retryDuration.getSeconds());

    retryDuration = retryDuration.plus(INITIAL_WAIT);
    if (retryDuration.compareTo(MAXIMUM_WAIT) > 0) {
      retryDuration = MAXIMUM_WAIT;
    }
  }

  /**
   * Converts a SAM-specific ApiException to the common API exception, based on the HTTP status
   * code.
   */
  private static ErrorReportException convertSAMExToCommonEx(final ApiException samEx) {
    logger.warn("SAM client exception code: {}", samEx.getCode());
    logger.warn("SAM client exception message: {}", samEx.getMessage());
    logger.warn("SAM client exception details: {}", samEx.getResponseBody());

    // Sometimes the sam message is buried one level down inside of the error report object.
    // If we find an empty message then we try to deserialize the error report and use that message.
    String message = samEx.getMessage();
    if (StringUtils.isEmpty(message)) {
      try {
        ErrorReport errorReport =
            objectMapper.readValue(samEx.getResponseBody(), ErrorReport.class);
        message = errorReport.getMessage();
      } catch (JsonProcessingException ex) {
        logger.debug("Unable to deserialize sam exception response body");
      }
    }

    switch (samEx.getCode()) {
      case HttpStatusCodes.STATUS_CODE_BAD_REQUEST:
        {
          return new BadRequestException(message, samEx);
        }
      case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED:
        {
          return new UnauthorizedException(message, samEx);
        }
      case HttpStatusCodes.STATUS_CODE_FORBIDDEN:
        {
          return new ForbiddenException(message, samEx);
        }
      case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
        {
          return new NotFoundException(message, samEx);
        }
      case HttpStatusCodes.STATUS_CODE_CONFLICT:
        {
          return new ConflictException(message, samEx);
        }
      case HttpStatusCodes.STATUS_CODE_SERVER_ERROR:
        {
          return new InternalServerErrorException(message, samEx);
        }
        // note that SAM does not use a 501 NOT_IMPLEMENTED status code, so that case is skipped
        // here
      default:
        {
          return new InternalServerErrorException(message, samEx);
        }
    }
  }
}
