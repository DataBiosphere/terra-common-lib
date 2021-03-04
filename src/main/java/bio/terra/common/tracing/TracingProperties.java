package bio.terra.common.tracing;

import com.google.common.collect.Lists;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "terra.common.tracing")
public class TracingProperties {
  /** Rate of trace sampling, 0.0 - 1.0 */
  private double probability = 0.10;

  /** Whether Stackdriver tracing export is enabled at all */
  private boolean stackdriverExportEnabled = true;

  /**
   * What HTTP URL patterns to enable tracing for. An empty list enables tracing for all HTTP
   * requests.
   */
  private List<String> urlPatterns = Lists.newArrayList("/api/*");

  public double getProbability() {
    return probability;
  }

  public void setProbability(double probability) {
    this.probability = probability;
  }

  public boolean getStackdriverExportEnabled() {
    return stackdriverExportEnabled;
  }

  public void setStackdriverExportEnabled(boolean stackdriverExportEnabled) {
    this.stackdriverExportEnabled = stackdriverExportEnabled;
  }

  public List<String> getUrlPatterns() {
    return urlPatterns;
  }

  public void setUrlPatterns(List<String> urlPatterns) {
    this.urlPatterns = urlPatterns;
  }
}
