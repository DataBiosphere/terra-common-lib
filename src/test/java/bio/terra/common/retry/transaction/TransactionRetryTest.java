package bio.terra.common.retry.transaction;

import bio.terra.common.logging.LoggingTestApplication;
import bio.terra.common.sam.SamRetry;
import com.google.api.client.http.HttpStatusCodes;
import org.broadinstitute.dsde.workbench.client.sam.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

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
    assertThrows(
        Exception.class,
        () -> transactionRetryProbe.throwMe(new Exception("test")));

    assertEquals(1, transactionRetryProbe.getCount());
  }
}
