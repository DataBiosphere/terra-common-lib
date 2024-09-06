package bio.terra.common.db;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.PessimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test for {@link DatabaseRetryUtils} */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DatabaseRetryUtilsTest {
  private static final PessimisticLockingFailureException RETRY_EXCEPTION =
      new PessimisticLockingFailureException("test");
  private static final Duration RETRY_SLEEP = Duration.ofMillis(1);

  @Mock DatabaseRetryUtils.DatabaseOperation<Boolean> mockDatabaseOperation;

  @Test
  void succeedAfterRetry() throws InterruptedException {
    // Throw retryable exception 3 times then succeed.
    when(mockDatabaseOperation.execute())
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenThrow(RETRY_EXCEPTION)
        .thenReturn(true);
    assertTrue(DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, RETRY_SLEEP, 10));
    verify(mockDatabaseOperation, times(4)).execute();
  }

  @Test
  void exceedMaxRetry() {
    when(mockDatabaseOperation.execute()).thenThrow(RETRY_EXCEPTION);
    DataAccessException finalException =
        assertThrows(
            DataAccessException.class,
            () -> DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, RETRY_SLEEP, 3));
    verify(mockDatabaseOperation, times(3)).execute();
    assertEquals(RETRY_EXCEPTION, finalException);
  }

  @Test
  void nonRetryableExecute() {
    when(mockDatabaseOperation.execute()).thenThrow(new RuntimeException("non-retryable"));
    RuntimeException finalException =
        assertThrows(
            RuntimeException.class,
            () -> DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, RETRY_SLEEP, 3));

    verify(mockDatabaseOperation, times(1)).execute();
    assertEquals("non-retryable", finalException.getMessage());
  }

  @Test
  void zeroMaxAttemptsInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, RETRY_SLEEP, 0));
  }
}
