package bio.terra.common.sam;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.api.client.http.HttpStatusCodes;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag("unit")
public class SamRetryTest {

  private static Logger logger = LoggerFactory.getLogger(SamRetryTest.class);
  private int count;

  @BeforeEach
  public void setup() {
    count = 0;
  }

  @Test
  public void testRetryTimeout() throws Exception {
    assertThrows(
        ApiException.class,
        () -> SamRetry.retry(() -> testRetryFinishInner(100), Duration.ofSeconds(10)));
  }

  @Test
  public void testRetryFinish() throws Exception {
    SamRetry.retry(() -> testRetryFinishInner(2));
  }

  @Test
  public void testRetrySamError() throws Exception {
    assertThrows(ApiException.class, () -> SamRetry.retry(() -> testRetryThrows()));
  }

  @Test
  public void testRetryVoidTimeout() throws Exception {
    assertThrows(
        // The SamRetry class will catch timeouts and throw a SamApiException that wraps the initial
        // failure from Sam that caused us to need to retry.
        ApiException.class,
        () -> SamRetry.retry(() -> testRetryVoidFinishInner(100), Duration.ofSeconds(10)));
  }

  @Test
  public void testRetryVoidFinish() throws Exception {
    SamRetry.retry(() -> testRetryVoidFinishInner(2));
  }

  @Test
  void testInterruptedExceptionThrown() throws Exception {
    AtomicBoolean gotInterruptedException = new AtomicBoolean(false);
    Thread execThread =
        new Thread(
            () -> {
              try {
                SamRetry.retry(() -> testRetryFinishInner(5));
              } catch (InterruptedException exception) {
                gotInterruptedException.set(true);
              } catch (ApiException apiException) {
                gotInterruptedException.set(false);
              }
            });
    execThread.start();
    // Sleep to ensure we are within the first the first sleep of SamRetry.
    TimeUnit.SECONDS.sleep(3);
    execThread.interrupt();
    // Sleep to ensure the interrupt has time to propagate.
    TimeUnit.SECONDS.sleep(1);
    assertTrue(gotInterruptedException.get());
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
