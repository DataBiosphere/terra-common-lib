package bio.terra.common.retry;

import bio.terra.common.db.DatabaseRetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.retry.RetryContext;
import org.springframework.retry.backoff.BackOffContext;
import org.springframework.retry.backoff.BackOffInterruptedException;
import org.springframework.retry.backoff.BackOffPolicy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BackOffPolicy that delegates to a number of nested BackOffPolicies. The BackOffPolicy chosen is
 * determined by the exception being handled. If more than one policy matches, the first is used.
 */
public class CompositeBackOffPolicy implements BackOffPolicy {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryUtils.class);

  // LinkedHashMap to ensure consistent behavior when an exception matches more than 1 policy
  private final LinkedHashMap<BinaryExceptionClassifier, BackOffPolicy> backOffPoliciesByClassifier;

  public CompositeBackOffPolicy(
      LinkedHashMap<BinaryExceptionClassifier, BackOffPolicy> backOffPoliciesByClassifier) {
    this.backOffPoliciesByClassifier = backOffPoliciesByClassifier;
  }

  @Override
  public BackOffContext start(RetryContext context) {
    Map<BinaryExceptionClassifier, BackOffContext> backOffContextMap = new HashMap<>();
    this.backOffPoliciesByClassifier.forEach(
        (classifier, policy) -> backOffContextMap.put(classifier, policy.start(context)));
    return new BackOffContextByClassifier(context, backOffContextMap);
  }

  @Override
  public void backOff(BackOffContext backOffContext) throws BackOffInterruptedException {
    BackOffContextByClassifier backOffContextByClassifier =
        (BackOffContextByClassifier) backOffContext;

    for (BinaryExceptionClassifier classifier :
        backOffContextByClassifier.backOffContexts.keySet()) {
      if (classifier.classify(backOffContextByClassifier.retryContext.getLastThrowable())) {
        logger.debug(
            "retrying",
            Map.of(
                "exception",
                backOffContextByClassifier.retryContext.getLastThrowable().getClass().getName(),
                "exceptionMessage",
                backOffContextByClassifier.retryContext.getLastThrowable().getMessage(),
                "failedTrialCount",
                backOffContextByClassifier.retryContext.getRetryCount()));

        backOffPoliciesByClassifier
            .get(classifier)
            .backOff(backOffContextByClassifier.backOffContexts.get(classifier));
        break; // don't back off for any further matching back off policies
      }
    }
  }

  /**
   * Class to keep track of retry context and a back off context for each nested back off policy.
   */
  private static class BackOffContextByClassifier implements BackOffContext {
    private final RetryContext retryContext;
    private final Map<BinaryExceptionClassifier, BackOffContext> backOffContexts;

    private BackOffContextByClassifier(
        RetryContext retryContext, Map<BinaryExceptionClassifier, BackOffContext> backOffContexts) {
      this.retryContext = retryContext;
      this.backOffContexts = backOffContexts;
    }
  }
}
