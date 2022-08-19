package bio.terra.common.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataAccessException;

/** Test for {@link DatabaseRetryUtils} */
@Tag("unit")
public class DatabaseRetryUtilsTest {
  private static final CannotSerializeTransactionException RETRY_EXCEPTION =
      new CannotSerializeTransactionException("test");

  @SuppressWarnings("unchecked")
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
    DataAccessException finalException =
        assertThrows(
            DataAccessException.class,
            () ->
                DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 3));
    verify(mockDatabaseOperation, times(3)).execute();
    assertEquals(RETRY_EXCEPTION, finalException);
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

  @Test
  public void zeroMaxAttemptsInvalid() throws Exception {
    when(mockDatabaseOperation.execute()).thenReturn(true);
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseRetryUtils.executeAndRetry(mockDatabaseOperation, Duration.ofMillis(1), 0));
  }
}
