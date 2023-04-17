package bio.terra.common.flagsmith;

import com.flagsmith.FlagsmithClient;
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

  public boolean isFeatureEnabled(String feature, Optional<Boolean> defaultValue) {
    if (!flagsmithProperties.getEnabled()) {
      LOGGER.info("Flagsmith is not enabled, use default value");
      return defaultValue.orElse(false);
    }

    var flagsmith =
        FlagsmithClient.newBuilder()
            .setApiKey(flagsmithProperties.getServerSideApiKey())
            .withApiUrl(flagsmithProperties.getApiUrl())
            .build();

    Flags flags;
    try {
      flags = flagsmith.getEnvironmentFlags();
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Failed to get environment flags", e);
      return defaultValue.orElse(false);
    }
    try {
      return flags.isFeatureEnabled(feature);
    } catch (FlagsmithClientError e) {
      LOGGER.warn("Failed to get the feature state of {}", feature, e);
      return defaultValue.orElse(false);
    }
  }
}
