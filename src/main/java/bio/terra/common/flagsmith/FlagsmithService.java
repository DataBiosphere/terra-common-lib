package bio.terra.common.flagsmith;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.config.FlagsmithCacheConfig;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlagsmithService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlagsmithService.class);

  private final FlagsmithProperties flagsmithProperties;
  public static final Duration DEFAULT_RETRY_TOTAL_DURATION = Duration.ofSeconds(60);
  public static final Duration DEFAULT_RETRY_SLEEP_DURATION = Duration.ofSeconds(10);

  @Autowired
  FlagsmithService(FlagsmithProperties flagsmithProperties) {
    this.flagsmithProperties = flagsmithProperties;
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   *
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature) {
    if (!flagsmithProperties.getEnabled()) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }

    FlagsmithCacheConfig.Builder cacheConfig =
        FlagsmithCacheConfig.newBuilder().expireAfterAccess(12, TimeUnit.HOURS);
    if (StringUtils.isNotEmpty(flagsmithProperties.getEnvCacheKey())) {
      cacheConfig.enableEnvLevelCaching(flagsmithProperties.getEnvCacheKey());
    }
    var flagsmith =
        FlagsmithClient.newBuilder()
            .setApiKey(flagsmithProperties.getServerSideApiKey())
            .withApiUrl(flagsmithProperties.getApiUrl())
            .withCache(cacheConfig.build())
            .build();

    try {
      Flags flags = getWithRetryOnException(flagsmith::getEnvironmentFlags);
      return Optional.of(flags.isFeatureEnabled(feature));
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Feature {} not found in {}", feature, flagsmithProperties.getApiUrl(), e);
    } catch (Exception e) {
      LOGGER.warn("Failed to fetch feature {}", feature, e);
    }
    return Optional.empty();
  }

  private static <T> T getWithRetryOnException(SupplierWithException<T> supplier) throws Exception {

    T result;
    Instant endTime = Instant.now().plus(DEFAULT_RETRY_TOTAL_DURATION);
    Duration sleepDuration = DEFAULT_RETRY_SLEEP_DURATION;

    while (true) {
      try {
        result = supplier.get();
        break;
      } catch (Exception e) {
        // If we are out of time or the exception is not retryable
        if (Instant.now().isAfter(endTime) || !(e instanceof FlagsmithClientError)) {
          throw e;
        }
        LOGGER.info(
            "Exception \"{}\". Waiting {} seconds. End time is {}",
            e.getMessage(),
            sleepDuration.toSeconds(),
            endTime);
        TimeUnit.MILLISECONDS.sleep(sleepDuration.toMillis());
      }
    }
    return result;
  }

  /**
   * Supplier that can throw
   *
   * @param <T> return type for the non-throw case
   */
  @FunctionalInterface
  private interface SupplierWithException<T> {
    T get() throws Exception;
  }
}
