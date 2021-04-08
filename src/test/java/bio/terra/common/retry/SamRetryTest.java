package bio.terra.common.retry;

import static org.junit.Assert.assertThrows;

import bio.terra.common.exception.InternalServerErrorException;
import bio.terra.common.exception.SamApiException;
import com.google.api.client.http.HttpStatusCodes;
import java.time.Duration;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class SamRetryTest {

  private int count;

  @BeforeEach
  public void setup() {
    count = 0;
  }

  @Test
  public void testRetryTimeout() throws Exception {
    assertThrows(
        SamApiException.class,
        () -> SamRetry.retry(() -> testRetryFinishInner(100), Duration.ofSeconds(10)));
  }

  @Test
  public void testRetryFinish() throws Exception {
    SamRetry.retry(() -> testRetryFinishInner(2));
  }

  @Test
  public void testRetrySamError() throws Exception {
    assertThrows(
        SamApiException.class,
        () -> SamRetry.retry(() -> testRetryThrows()));
  }

  @Test
  public void testRetryVoidTimeout() throws Exception {
    assertThrows(
        // The SamRetry class will catch timeouts and throw a SamApiException that wraps the initial
        // failure from Sam that caused us to need to retry.
        SamApiException.class,
        () -> SamRetry.retry(() -> testRetryVoidFinishInner(100), Duration.ofSeconds(10)));
  }

  @Test
  public void testRetryVoidFinish() throws Exception {
    SamRetry.retry(() -> testRetryVoidFinishInner(2));
  }

  private boolean testRetryFinishInner(int failCount) throws ApiException {
    if (count < failCount) {
      count++;
      throw new ApiException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "testing");
    }
    return true;
  }

  private void testRetryVoidFinishInner(int failCount) throws ApiException {
    if (count < failCount) {
      count++;
      throw new ApiException(HttpStatusCodes.STATUS_CODE_SERVER_ERROR, "testing");
    }
  }

  private boolean testRetryThrows() throws ApiException {
    throw new ApiException(HttpStatusCodes.STATUS_CODE_NOT_FOUND, "testing");
  }
}
