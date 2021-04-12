package bio.terra.common.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Wrapper exception for non-200 responses from calls to Sam. Does not extend ErrorReportException
 * so that SamApiException is a checked exception type. Can be converted to an ErrorReportException
 * subtype using the toErrorReportException function.
 */
public class SamApiException extends Exception {
  private ApiException apiException;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static Logger logger = LoggerFactory.getLogger(SamApiException.class);

  public static SamApiException createSamApiException(ApiException apiException) {
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
    return new SamApiException(message, apiException);
  }

  private SamApiException(String message, ApiException apiException) {
    super("Error from Sam: " + message, apiException);
    this.apiException = apiException;
  }

  /** Get the HTTP status code of the underlying response from Sam. */
  public HttpStatus getStatusCode() {
    return Optional.ofNullable(HttpStatus.resolve(apiException.getCode()))
        .orElse(HttpStatus.valueOf(HttpStatusCodes.STATUS_CODE_SERVER_ERROR));
  }

  public ErrorReportException toErrorReportException() {
    switch (apiException.getCode()) {
      case HttpStatusCodes.STATUS_CODE_BAD_REQUEST:
        {
          return new BadRequestException(this.getMessage(), this.getCause());
        }
      case HttpStatusCodes.STATUS_CODE_UNAUTHORIZED:
        {
          return new UnauthorizedException(this.getMessage(), this.getCause());
        }
      case HttpStatusCodes.STATUS_CODE_FORBIDDEN:
        {
          return new ForbiddenException(this.getMessage(), this.getCause());
        }
      case HttpStatusCodes.STATUS_CODE_NOT_FOUND:
        {
          return new NotFoundException(this.getMessage(), this.getCause());
        }
      case HttpStatusCodes.STATUS_CODE_CONFLICT:
        {
          return new ConflictException(this.getMessage(), this.getCause());
        }
      case HttpStatusCodes.STATUS_CODE_SERVER_ERROR:
        {
          return new InternalServerErrorException(this.getMessage(), this.getCause());
        }
        // note that SAM does not use a 501 NOT_IMPLEMENTED status code, so that case is skipped
        // here
      default:
        {
          return new InternalServerErrorException(this.getMessage(), this.getCause());
        }
    }
  }
}
