package bio.terra.common.retry.transaction;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;

@SpringBootTest(classes = TransactionRetryTestApplication.class)
@ActiveProfiles("retry-transaction-test")
@Tag("unit")
class TransactionRetryTest {

  @Autowired private TransactionRetryProbe transactionRetryProbe;

  @BeforeEach
  public void setup() {
    transactionRetryProbe.reset();
  }

  @Test
  void testFastRetry() {
    var exception = new PessimisticLockingFailureException("test");
    assertThrows(
        PessimisticLockingFailureException.class, () -> transactionRetryProbe.throwMe(exception));

    assertEquals(10, transactionRetryProbe.getCount());
  }

  @Test
  void testSlowRetry() {
    var exception = new CannotCreateTransactionException("test");
    assertThrows(
        CannotCreateTransactionException.class, () -> transactionRetryProbe.throwMe(exception));

    assertEquals(4, transactionRetryProbe.getCount());
  }

  @Test
  void testNoRetry() {
    var exception = new Exception("test");
    assertThrows(Exception.class, () -> transactionRetryProbe.throwMe(exception));

    assertEquals(1, transactionRetryProbe.getCount());
  }
}
