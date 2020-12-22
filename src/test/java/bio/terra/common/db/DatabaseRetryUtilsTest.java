package bio.terra.common.db;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.dao.CannotSerializeTransactionException;

/** Test for {@link DatabaseRetryUtils} */
@Tag("unit")
public class DatabaseRetryUtilsTest {
  private static final CannotSerializeTransactionException RETRY_EXCEPTION =
      new CannotSerializeTransactionException("test");

  @Mock
  DatabaseRetryUtils.DatabaseOperation<Boolean> mockDatabaseOperation =
      mock(DatabaseRetryUtils.DatabaseOperation.class);

  @Test
  public void succeedAfterRetry() throws Exception {
    // Throw retryable exception 3 times then succeed.
    when(mockDatabaseOperation.execute())
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenReturn(true);
    assertTrue(
        () -> {
          try {
            return DatabaseRetryUtils.executeAndRetry(
                mockDatabaseOperation, Duration.ofMillis(1), 10);
          } catch (InterruptedException e) {
            fail("Should not throw exception");
            return false;
          }
        });
    verify(mockDatabaseOperation, times(4)).execute();
  }

  @Test
  public void exceedMaxRetry() throws Exception {
    when(mockDatabaseOperation.execute()).thenThrow(RETRY_EXCEPTION);
    InterruptedException finalException =
        assertThrows(
            InterruptedException.class,
            () ->
                DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 3));
    verify(mockDatabaseOperation, times(3)).execute();
    assertEquals("Exceeds maximum number of retries.", finalException.getMessage());
  }

  @Test
  public void nonRetryableExecute() throws Exception {
    when(mockDatabaseOperation.execute()).thenThrow(new RuntimeException("non-retryable"));
    RuntimeException finalException =
        assertThrows(
            RuntimeException.class,
            () ->
                DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 3));

    verify(mockDatabaseOperation, times(1)).execute();
    assertEquals("non-retryable", finalException.getMessage());
  }
}
