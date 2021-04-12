package bio.terra.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import java.util.HashMap;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Test for {@link SamApiException} */
@Tag("unit")
public class SamApiExceptionTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void extractsMessageFromSamTopLevel() {
    String message = "Top level message";
    ApiException apiException = new ApiException("Top level message");
    SamApiException samApiException = SamApiException.createSamApiException(apiException);
    assertEquals("Error from Sam: " + message, samApiException.getMessage());
  }

  @Test
  public void extractsMessageBuriedInSam() {
    String message = "Buried message";
    ErrorReport errorReport = new ErrorReport().message(message);
    try {
      String errorReportString = objectMapper.writeValueAsString(errorReport);
      ApiException apiException =
          new ApiException(
              "", HttpStatusCodes.STATUS_CODE_BAD_REQUEST, new HashMap<>(), errorReportString);
      SamApiException samApiException = SamApiException.createSamApiException(apiException);
      assertEquals("Error from Sam: " + message, samApiException.getMessage());
    } catch (JsonProcessingException e) {
      // FAIL
    }
  }

  @Test
  public void toErrorReportExceptionBadRequest() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_BAD_REQUEST, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof BadRequestException);
  }

  @Test
  public void toErrorReportExceptionUnauthorized() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof UnauthorizedException);
  }

  @Test
  public void toErrorReportExceptionForbidden() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_FORBIDDEN, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof ForbiddenException);
  }

  @Test
  public void toErrorReportExceptionNotFound() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof NotFoundException);
  }

  @Test
  public void toErrorReportExceptionConflict() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_CONFLICT, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof ConflictException);
  }

  @Test
  public void toErrorReportExceptionServerError() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(
                new ApiException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "test"))
            .toErrorReportException();
    assertTrue(errorReportException instanceof InternalServerErrorException);
  }

  @Test
  public void toErrorReportExceptionNoStatusCode() {
    ErrorReportException errorReportException =
        SamApiException.createSamApiException(new ApiException("test")).toErrorReportException();
    assertTrue(errorReportException instanceof InternalServerErrorException);
  }
}
