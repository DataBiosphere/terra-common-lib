package bio.terra.common.sam.exception;

import bio.terra.common.exception.ErrorReportException;
import bio.terra.common.sam.SamRetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating common Sam exceptions. This class pulls the error message out of the
 * Sam ApiException, which could be buried a layer down, and then inspects the HTTP Status code to
 * generate the right type of exception.
 */
public class SamExceptionFactory {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static Logger logger = LoggerFactory.getLogger(SamExceptionFactory.class);

  public static ErrorReportException create(ApiException apiException) {
    return create(null, apiException);
  }

  public static ErrorReportException create(String messagePrefix, ApiException apiException) {
    // Sometimes the sam message is buried one level down inside of the error report object.
    // If we find an empty message then we try to deserialize the error report and use that message.
    String message = apiException.getMessage();
    if (StringUtils.isEmpty(message)) {
      try {
        ErrorReport errorReport =
            objectMapper.readValue(apiException.getResponseBody(), ErrorReport.class);
        message = errorReport.getMessage();
      } catch (JsonProcessingException ex) {
        logger.debug("Unable to deserialize sam exception response body");
      }
    }
    if (!StringUtils.isEmpty(messagePrefix)) {
      message = messagePrefix + ": " + message;
    }

    if (SamRetry.isTimeoutException(apiException)) {
      return new SamTimeoutException(message, apiException);
    }
    switch (apiException.getCode()) {
      case HttpStatusCodes.STATUS_CODE_BAD_REQUEST:
        return new SamBadRequestException(message, apiException);
      case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED:
        return new SamUnauthorizedException(message, apiException);
      case HttpStatusCodes.STATUS_CODE_FORBIDDEN:
        return new SamForbiddenException(message, apiException);
      case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
        return new SamNotFoundException(message, apiException);
      case HttpStatusCodes.STATUS_CODE_CONFLICT:
        return new SamConflictException(message, apiException);
      case HttpStatusCodes.STATUS_CODE_SERVER_ERROR:
        return new SamInternalServerErrorException(message, apiException);
        // note that SAM does not use a 501 NOT_IMPLEMENTED status code, so that case is skipped
        // here
      default:
        logger.warn("Got an unexpected response code from Sam: " + apiException);
        return new SamInternalServerErrorException(message, apiException);
    }
  }

  /**
   * A SamInterruptedException is thrown when an Interrupted Exception is raised while Sam is
   * retrying. This should be used in contexts outside of Stairway. In Stairway, the
   * InterruptedException itself should be raised.
   */
  public static ErrorReportException create(
      String messagePrefix, InterruptedException interruptedException) {
    return new SamInterruptedException(messagePrefix, interruptedException);
  }
}
