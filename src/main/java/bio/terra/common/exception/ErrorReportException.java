package bio.terra.common.exception;

import com.google.common.html.HtmlEscapers;
import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpStatus;

/**
 * This base class has data that corresponds to the ErrorReport model generated from the OpenAPI
 * yaml. {@link AbstractGlobalExceptionHandler} uses this class to build appropriate ErrorReport
 * REST response.
 */
public abstract class ErrorReportException extends RuntimeException {
  private final List<String> causes;
  private final HttpStatus statusCode;

  public ErrorReportException(String message) {
    super(encodeMessage(message));
    this.causes = null;
    this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ErrorReportException(String message, Throwable cause) {
    super(encodeMessage(message), cause);
    this.causes = null;
    this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ErrorReportException(Throwable cause) {
    super(cause);
    this.causes = null;
    this.statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
  }

  public ErrorReportException(Throwable cause, HttpStatus statusCode) {
    super(cause);
    this.causes = null;
    this.statusCode = statusCode;
  }

  public ErrorReportException(String message, List<String> causes, HttpStatus statusCode) {
    super(encodeMessage(message));
    this.causes = causes;
    this.statusCode = statusCode;
  }

  public ErrorReportException(
      String message, Throwable cause, List<String> causes, HttpStatus statusCode) {
    super(encodeMessage(message), cause);
    this.causes = causes;
    this.statusCode = statusCode;
  }

  public List<String> getCauses() {
    return causes;
  }

  public HttpStatus getStatusCode() {
    return statusCode;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("causes", causes)
        .append("statusCode", statusCode)
        .toString();
  }

  private static String encodeMessage(String message) {
    return HtmlEscapers.htmlEscaper().escape(message);
  }
}
