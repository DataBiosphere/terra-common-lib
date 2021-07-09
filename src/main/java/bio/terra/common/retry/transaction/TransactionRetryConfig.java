package bio.terra.common.retry.transaction;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.classify.BinaryExceptionClassifier;

/**
 * Configuration settings for how database transactions are retried. There are 2 classes of retries,
 * fast and slow. Fast retries usually measure in milliseconds with a constant retry period (with
 * some random jitter) and many attempts. Slow retries usually measure in seconds with an
 * exponential back off and few attempts.
 */
@ConfigurationProperties(prefix = "terra.common.retry.transaction")
public class TransactionRetryConfig implements InitializingBean {
  private List<Class<? extends Throwable>> fastRetryExceptions;
  private int fastRetryMaxAttempts;
  private Duration fastRetryMinBackOffPeriod;
  private Duration fastRetryMaxBackOffPeriod;

  private List<Class<? extends Throwable>> slowRetryExceptions;
  private int slowRetryMaxAttempts;
  private Duration slowRetryInitialInterval;
  private double slowRetryMultiplier;

  private BinaryExceptionClassifier slowRetryExceptionClassifier;
  private BinaryExceptionClassifier fastRetryExceptionClassifier;

  /** Exceptions to retry FAST */
  public List<Class<? extends Throwable>> getFastRetryExceptions() {
    return fastRetryExceptions;
  }

  public void setFastRetryExceptions(List<Class<? extends Throwable>> fastRetryExceptions) {
    this.fastRetryExceptions = fastRetryExceptions;
  }

  /** Max attempts for FAST retries (including initial attempt) */
  public int getFastRetryMaxAttempts() {
    return fastRetryMaxAttempts;
  }

  public void setFastRetryMaxAttempts(int fastRetryMaxAttempts) {
    this.fastRetryMaxAttempts = fastRetryMaxAttempts;
  }

  /** Minimum time to wait before the next FAST retry */
  public Duration getFastRetryMinBackOffPeriod() {
    return fastRetryMinBackOffPeriod;
  }

  public void setFastRetryMinBackOffPeriod(Duration fastRetryMinBackOffPeriod) {
    this.fastRetryMinBackOffPeriod = fastRetryMinBackOffPeriod;
  }

  /** Maximum time to wait before the next FAST retry */
  public Duration getFastRetryMaxBackOffPeriod() {
    return fastRetryMaxBackOffPeriod;
  }

  public void setFastRetryMaxBackOffPeriod(Duration fastRetryMaxBackOffPeriod) {
    this.fastRetryMaxBackOffPeriod = fastRetryMaxBackOffPeriod;
  }

  /** Exceptions to retry SLOW */
  public List<Class<? extends Throwable>> getSlowRetryExceptions() {
    return slowRetryExceptions;
  }

  public void setSlowRetryExceptions(List<Class<? extends Throwable>> slowRetryExceptions) {
    this.slowRetryExceptions = slowRetryExceptions;
  }

  /** Max attempts for SLOW retries (including initial attempt) */
  public int getSlowRetryMaxAttempts() {
    return slowRetryMaxAttempts;
  }

  public void setSlowRetryMaxAttempts(int slowRetryMaxAttempts) {
    this.slowRetryMaxAttempts = slowRetryMaxAttempts;
  }

  /** Interval to wait for the initial SLOW retry */
  public Duration getSlowRetryInitialInterval() {
    return slowRetryInitialInterval;
  }

  public void setSlowRetryInitialInterval(Duration slowRetryInitialInterval) {
    this.slowRetryInitialInterval = slowRetryInitialInterval;
  }

  /** Multiplier applied to the last SLOW trial interval */
  public double getSlowRetryMultiplier() {
    return slowRetryMultiplier;
  }

  public void setSlowRetryMultiplier(double slowRetryMultiplier) {
    this.slowRetryMultiplier = slowRetryMultiplier;
  }

  /** BinaryExceptionClassifier created using slowRetryExceptions */
  public BinaryExceptionClassifier getSlowRetryExceptionClassifier() {
    return slowRetryExceptionClassifier;
  }

  /** BinaryExceptionClassifier created using fastRetryExceptions */
  public BinaryExceptionClassifier getFastRetryExceptionClassifier() {
    return fastRetryExceptionClassifier;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Objects.requireNonNull(this.fastRetryExceptions, "fastRetryExceptions must be specified");
    Objects.requireNonNull(this.slowRetryExceptions, "slowRetryExceptions must be specified");

    this.fastRetryExceptionClassifier = new BinaryExceptionClassifier(this.fastRetryExceptions);
    this.slowRetryExceptionClassifier = new BinaryExceptionClassifier(this.slowRetryExceptions);
  }
}
