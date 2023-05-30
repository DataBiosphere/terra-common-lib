package bio.terra.common.flagsmith;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static final Duration DEFAULT_RETRY_TOTAL_DURATION = Duration.ofSeconds(60);
  private static final Duration DEFAULT_RETRY_SLEEP_DURATION = Duration.ofSeconds(10);

  private final FlagsmithProperties flagsmithProperties;
  private final ObjectMapper objectMapper;

  @Autowired
  FlagsmithService(FlagsmithProperties flagsmithProperties, ObjectMapper objectMapper) {
    this.flagsmithProperties = flagsmithProperties;
    this.objectMapper = objectMapper;
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   *
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature) throws Exception {
    if (!flagsmithProperties.getEnabled()) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }
    var flagsmith = getFlagsmithClient();

    try {
      Flags flags = getWithRetryOnException(flagsmith::getEnvironmentFlags);
      return Optional.of(flags.isFeatureEnabled(feature));
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Feature {} not found in {}", feature, flagsmithProperties.getApiUrl(), e);
    } catch (Exception e) {
      LOGGER.warn("Something went wrong when fetching value for feature {}", feature, e);
      throw new FlagsmithFeatureFetchingException(
          String.format("Something went wrong when fetching value for feature %s", feature), e);
    }
    return Optional.empty();
  }

  /**
   * Get feature's value formatted as JSON.
   *
   * <p>If Flagsmith is unavailable, feature does not exist or the feature value does not exist,
   * return {@code Optional.empty()}.
   */
  public <T> Optional<T> getFeatureValueJson(String feature, Class<T> clazz) throws Exception {
    if (!flagsmithProperties.getEnabled()) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }
    var flagsmith = getFlagsmithClient();
    try {
      Flags flags = getWithRetryOnException(flagsmith::getEnvironmentFlags);
      Object value = flags.getFeatureValue(feature);
      if (value == null) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(value.toString(), clazz));
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Feature {} not found in {}", feature, flagsmithProperties.getApiUrl(), e);
    } catch (JsonMappingException e) {
      LOGGER.warn("Failed to deserialize value for feature {}", feature, e);
    } catch (Exception e) {
      LOGGER.warn("Something went wrong when fetching value for feature {}", feature, e);
      throw new FlagsmithFeatureFetchingException(
          String.format("Something went wrong when fetching value for feature %s", feature), e);
    }
    return Optional.empty();
  }

  private FlagsmithClient getFlagsmithClient() {
    FlagsmithCacheConfig.Builder cacheConfig =
        FlagsmithCacheConfig.newBuilder().expireAfterAccess(12, TimeUnit.HOURS);
    if (StringUtils.isNotEmpty(flagsmithProperties.getEnvCacheKey())) {
      cacheConfig.enableEnvLevelCaching(flagsmithProperties.getEnvCacheKey());
    }
    return FlagsmithClient.newBuilder()
        .setApiKey(flagsmithProperties.getServerSideApiKey())
        .withApiUrl(flagsmithProperties.getApiUrl())
        .withCache(cacheConfig.build())
        .enableLogging()
        .build();
  }

  private static <T> T getWithRetryOnException(SupplierWithException<T> supplier) throws Exception {

    T result;
    Instant endTime = Instant.now().plus(DEFAULT_RETRY_TOTAL_DURATION);

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
            "Exception \"{}\". Waiting 10 seconds. End time is {}", e.getMessage(), endTime);
        TimeUnit.MILLISECONDS.sleep(DEFAULT_RETRY_SLEEP_DURATION.toMillis());
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
