package bio.terra.common.flagsmith;

import com.flagsmith.FlagsmithClient;
import com.flagsmith.config.FlagsmithCacheConfig;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlagsmithService {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlagsmithService.class);

  @Autowired private final FlagsmithProperties flagsmithProperties;

  FlagsmithService(FlagsmithProperties flagsmithProperties) {
    this.flagsmithProperties = flagsmithProperties;
  }

  /**
   * If Flagsmith is unavailable or the feature does not exist, return {@code Optional.empty()}
   * @param feature the name of the feature
   */
  public Optional<Boolean> isFeatureEnabled(String feature) {
    if (!flagsmithProperties.getEnabled()) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return Optional.empty();
    }

    var flagsmith =
        FlagsmithClient.newBuilder()
            .setApiKey(flagsmithProperties.getServerSideApiKey())
            .withApiUrl(flagsmithProperties.getApiUrl())
            .withCache(FlagsmithCacheConfig.newBuilder().build())
            .build();

    Flags flags;
    try {
      flags = flagsmith.getEnvironmentFlags();
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Failed to get environment flags", e);
      return Optional.empty();
    }
    try {
      return Optional.of(flags.isFeatureEnabled(feature));
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Failed to get the feature state of {}", feature, e);
      return Optional.empty();
    }
  }
}
