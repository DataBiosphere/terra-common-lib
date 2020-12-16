package bio.terra.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;

/**
 * This module provides an abstract as a top-level exception handler for controllers. All exceptions that rise
 * through the controllers are caught in this handler. It converts the exceptions into standard
 * ErrorReport responses. The ErrorReport is defined in each server's openapi yaml file. To use this class, extern this
 * and implement generateErrorReport method. Sample code:
 *
 * <pre>
 * @RestControllerAdvice
 * public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler<ErrorReport> {
 *   @Override
 *   ErrorReport generateErrorReport(Throwable ex, HttpStatus statusCode, List<String> causes) {
 *     return new ErrorReport().message(ex.getMessage()).statusCode(statusCode.value()).causes(causes);
 *   }
 * }
 * </pre>
 */
abstract class AbstractGlobalExceptionHandler<T> {
  private final Logger logger = LoggerFactory.getLogger(AbstractGlobalExceptionHandler.class);

  // -- Error Report - one of our exceptions --
  @ExceptionHandler(ErrorReportException.class)
  public ResponseEntity<T> errorReportHandler(ErrorReportException ex) {
    return buildErrorReport(ex, ex.getStatusCode(), ex.getCauses());
  }

  // -- validation exceptions - we don't control the exception raised
  @ExceptionHandler({
          MethodArgumentNotValidException.class,
          IllegalArgumentException.class,
          NoHandlerFoundException.class
  })
  public ResponseEntity<T> validationExceptionHandler(Exception ex) {
    return buildErrorReport(ex, HttpStatus.BAD_REQUEST, null);
  }

  // -- catchall - log so we can understand what we have missed in the handlers above
  @ExceptionHandler(Exception.class)
  public ResponseEntity<T> catchallHandler(Exception ex) {
    logger.error("Exception caught by catchall hander", ex);
    return buildErrorReport(ex, HttpStatus.INTERNAL_SERVER_ERROR, null);
  }

  private ResponseEntity<T> buildErrorReport(
          Throwable ex, HttpStatus statusCode, List<String> causes) {
    StringBuilder combinedCauseString = new StringBuilder();
    logger.error("Global exception handler: catch stack", ex);
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      combinedCauseString.append("cause: " + cause.toString() + ", ");
    }
    logger.error("Global exception handler: " + combinedCauseString.toString(), ex);

    return new ResponseEntity<>(generateErrorReport(ex, statusCode, causes), statusCode);
  }

  abstract T generateErrorReport(Throwable ex, HttpStatus statusCode, List<String> causes);
}
