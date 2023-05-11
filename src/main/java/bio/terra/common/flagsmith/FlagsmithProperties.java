package bio.terra.common.flagsmith;

import javax.annotation.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terra.common.flagsmith")
public class FlagsmithProperties {

  private boolean enabled = false;
  private String serverSideApiKey;
  private String apiUrl;

  private String envCacheKey;

  public boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(boolean flagsmithEnabled) {
    this.enabled = flagsmithEnabled;
  }

  public @Nullable String getServerSideApiKey() {
    return serverSideApiKey;
  }

  public void setServerSideApiKey(String serverSideApiKey) {
    this.serverSideApiKey = serverSideApiKey;
  }

  public @Nullable String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public @Nullable String getEnvCacheKey() {
    return envCacheKey;
  }

  public void setEnvCacheKey(String envCacheKey) {
    this.envCacheKey = envCacheKey;
  }
}
