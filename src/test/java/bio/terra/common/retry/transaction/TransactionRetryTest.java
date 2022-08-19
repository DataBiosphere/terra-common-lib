package bio.terra.common.retry.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;

@SpringBootTest(classes = TransactionRetryTestApplication.class)
@ActiveProfiles("retry-transaction-test")
@Tag("unit")
public class TransactionRetryTest {

  @Autowired private TransactionRetryProbe transactionRetryProbe;

  @BeforeEach
  public void setup() {
    transactionRetryProbe.reset();
  }

  @Test
  public void testFastRetry() {
    assertThrows(
        CannotSerializeTransactionException.class,
        () -> transactionRetryProbe.throwMe(new CannotSerializeTransactionException("test")));

    assertEquals(10, transactionRetryProbe.getCount());
  }

  @Test
  public void testSlowRetry() {
    assertThrows(
        CannotCreateTransactionException.class,
        () -> transactionRetryProbe.throwMe(new CannotCreateTransactionException("test")));

    assertEquals(4, transactionRetryProbe.getCount());
  }

  @Test
  public void testNoRetry() {
    assertThrows(Exception.class, () -> transactionRetryProbe.throwMe(new Exception("test")));

    assertEquals(1, transactionRetryProbe.getCount());
  }
}
