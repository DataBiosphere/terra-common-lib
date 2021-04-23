package bio.terra.common.sam.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.exception.ErrorReportException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.HttpStatusCodes;
import java.util.HashMap;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.broadinstitute.dsde.workbench.client.sam.model.ErrorReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Test for {@link bio.terra.common.sam.exception.SamExceptionFactory} */
@Tag("unit")
public class SamExceptionFactoryTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void extractsMessageFromSamTopLevel() {
    String message = "Top level message";
    ApiException apiException = new ApiException("Top level message");
    ErrorReportException errorReportException = SamExceptionFactory.create(apiException);
    assertEquals(message, errorReportException.getMessage());
  }

  @Test
  public void extractsMessageBuriedInSam() throws Exception {
    String message = "Buried message";
    ErrorReport errorReport = new ErrorReport().message(message);
    String errorReportString = objectMapper.writeValueAsString(errorReport);
    ApiException apiException =
        new ApiException(
            "", HttpStatusCodes.STATUS_CODE_BAD_REQUEST, new HashMap<>(), errorReportString);
    ErrorReportException errorReportException = SamExceptionFactory.create(apiException);
    assertEquals(message, errorReportException.getMessage());
    assertTrue(errorReportException instanceof SamBadRequestException);
  }

  @Test
  public void UnauthorizedException() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(
            new ApiException(HttpStatusCodes.STATUS_CODE_UNAUTHORIZED, "test"));
    assertTrue(errorReportException instanceof SamUnauthorizedException);
  }

  @Test
  public void ForbiddenException() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(new ApiException(HttpStatusCodes.STATUS_CODE_FORBIDDEN, "test"));
    assertTrue(errorReportException instanceof SamForbiddenException);
  }

  @Test
  public void NotFoundException() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(new ApiException(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "test"));
    assertTrue(errorReportException instanceof SamNotFoundException);
  }

  @Test
  public void ConflictException() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(new ApiException(HttpStatusCodes.STATUS_CODE_CONFLICT, "test"));
    assertTrue(errorReportException instanceof SamConflictException);
  }

  @Test
  public void InternalServerErrorException() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(
            new ApiException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "test"));
    assertTrue(errorReportException instanceof SamInternalServerErrorException);
  }

  @Test
  public void toErrorReportExceptionNoStatusCode() {
    ErrorReportException errorReportException =
        SamExceptionFactory.create(new ApiException("test"));
    assertTrue(errorReportException instanceof SamInternalServerErrorException);
  }
}
