package bio.terra.common.retry.transaction;

import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TransactionRetryProbe {
  private AtomicInteger count = new AtomicInteger(0);

  @Retryable(interceptor = "transactionRetryInterceptor")
  public void throwMe(Exception e) throws Exception {
    count.incrementAndGet();
    throw e;
  }

  public int getCount() { return count.get(); }
  public void reset() { count.set(0); }
}
