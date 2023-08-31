package bio.terra.common.flagsmith;

import static org.slf4j.LoggerFactory.getLogger;

import bio.terra.common.exception.BadRequestException;
import com.flagsmith.exceptions.FlagsmithClientError;
import com.flagsmith.models.Flags;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class FlagsmithClient {
  private static final Logger LOGGER = getLogger(FlagsmithClient.class);
  private final com.flagsmith.FlagsmithClient flagsmith;

  /**
   * Create a new flagsmith client
   *
   * @param flagsmithApiUrl API Url for flagsmith service deployment
   * @param flagsmithClientSideKey flagsmith client side key
   */
  public FlagsmithClient(String flagsmithApiUrl, String flagsmithClientSideKey) {
    if (StringUtils.isEmpty(flagsmithApiUrl) || StringUtils.isEmpty(flagsmithClientSideKey)) {
      throw new BadRequestException("flagsmithApiUrl and flagsmithClientSideKey must be non-empty");
    }

    this.flagsmith =
        com.flagsmith.FlagsmithClient.newBuilder()
            .withApiUrl(flagsmithApiUrl)
            .setApiKey(flagsmithClientSideKey)
            .build();
  }

  /**
   * Check if a feature is enabled
   *
   * @param feature {@link Features}
   * @return true if the feature is enabled
   */
  public boolean isFeatureEnabled(Features feature) {
    return isFeatureEnabled(feature.featureName, /*userEmail=*/ null).orElse(false);
  }

  /**
   * Check if a feature is enabled
   *
   * @param feature {@link Features}
   * @param userEmail user email to check for
   * @return true if the feature is enabled for the user email
   */
  public boolean isFeatureEnabled(Features feature, @Nullable String userEmail) {
    return isFeatureEnabled(feature.featureName, userEmail).orElse(false);
  }

  /**
   * Check if a feature is enabled
   *
   * @param feature the name of the feature
   * @param userEmail user email to check for
   * @return {@code Optional.of(true)} if the feature is enabled for user email; {@code
   *     Optional.of(false)} if the feature is disabled for user email; {@code Optional.empty()} if
   *     the feature does not exist
   */
  public Optional<Boolean> isFeatureEnabled(String feature, @Nullable String userEmail) {
    try {
      return Optional.of(getFlags(userEmail).isFeatureEnabled(feature));
    } catch (Exception e) {
      LOGGER.debug("failed to fetch feature flag value", e);
      return Optional.empty();
    }
  }

  private Flags getFlags(String userEmail) throws FlagsmithClientError {
    if (userEmail != null) {
      flagsmith.getIdentityFlags(userEmail, Map.of("email_address", userEmail));
    }
    return flagsmith.getEnvironmentFlags();
  }

  // list of features
  public enum Features {
    AWS_ENABLED("vwb__aws_enabled"),
    GCP_ENABLED("vwb__gcp_enabled"),

    // CLI specific
    CLI_AUTH0_TOKEN_REFRESH_ENABLED("vwb__cli_token_refresh_enabled"),
    CLI_DATAPROC_ENABLED("vwb__cli_dataproc_enabled");

    private final String featureName;

    Features(String featureName) {
      this.featureName = featureName;
    }

    public String toString() {
      return featureName;
    }
  }
}
