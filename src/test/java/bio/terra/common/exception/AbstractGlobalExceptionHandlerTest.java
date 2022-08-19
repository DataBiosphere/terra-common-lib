package bio.terra.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Test for {@link AbstractGlobalExceptionHandler} */
@Tag("unit")
public class AbstractGlobalExceptionHandlerTest {
  private TestExceptionHandler exceptionHandler = new TestExceptionHandler();

  @Test
  public void internalErrorException() throws Exception {
    assertEquals(
        INTERNAL_SERVER_ERROR,
        exceptionHandler
            .errorReportHandler(new InternalServerErrorException("test"))
            .getStatusCode());
  }

  @Test
  public void apiException() throws Exception {
    assertEquals(
        INTERNAL_SERVER_ERROR,
        exceptionHandler.errorReportHandler(new ApiException("test")).getStatusCode());
  }

  @Test
  public void badRequestException() throws Exception {
    assertEquals(
        BAD_REQUEST,
        exceptionHandler.errorReportHandler(new BadRequestException("test")).getStatusCode());
  }

  @Test
  public void unauthorizedException() throws Exception {
    assertEquals(
        UNAUTHORIZED,
        exceptionHandler.errorReportHandler(new UnauthorizedException("test")).getStatusCode());
  }

  private static class TestExceptionHandler extends AbstractGlobalExceptionHandler<ErrorReport> {
    @Override
    public ErrorReport generateErrorReport(
        Throwable ex, HttpStatus statusCode, List<String> causes) {
      return new ErrorReport(statusCode);
    }
  }

  private static class ErrorReport {
    private final HttpStatus statusCode;

    ErrorReport(HttpStatus statusCode) {
      this.statusCode = statusCode;
    }
  }
}
