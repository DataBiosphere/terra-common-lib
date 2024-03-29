package bio.terra.common.exception;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.http.HttpStatus;

/**
 * This base class has data that corresponds to the ErrorReport model generated from the OpenAPI
 * yaml. {@link AbstractGlobalExceptionHandler} uses this class to build appropriate ErrorReport
 * REST response.
 */
public abstract class ErrorReportException extends RuntimeException {
  private static final HttpStatus DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

  private final List<String> causes;
  private final HttpStatus statusCode;

  public ErrorReportException(String message) {
    super(message);
    this.causes = Collections.emptyList();
    this.statusCode = DEFAULT_STATUS;
  }

  public ErrorReportException(String message, Throwable cause) {
    super(message, cause);
    this.causes = Collections.emptyList();
    this.statusCode = DEFAULT_STATUS;
  }

  public ErrorReportException(Throwable cause) {
    super(cause);
    this.causes = Collections.emptyList();
    this.statusCode = DEFAULT_STATUS;
  }

  public ErrorReportException(Throwable cause, HttpStatus statusCode) {
    super(cause);
    this.causes = Collections.emptyList();
    this.statusCode = Optional.ofNullable(statusCode).orElse(DEFAULT_STATUS);
  }

  public ErrorReportException(
      String message, @Nullable List<String> causes, @Nullable HttpStatus statusCode) {
    super(message);
    this.causes = Optional.ofNullable(causes).orElse(Collections.emptyList());
    this.statusCode = Optional.ofNullable(statusCode).orElse(DEFAULT_STATUS);
  }

  public ErrorReportException(
      String message,
      Throwable cause,
      @Nullable List<String> causes,
      @Nullable HttpStatus statusCode) {
    super(message, cause);
    this.causes = Optional.ofNullable(causes).orElse(Collections.emptyList());
    this.statusCode = Optional.ofNullable(statusCode).orElse(DEFAULT_STATUS);
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
}
