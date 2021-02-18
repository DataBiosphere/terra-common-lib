package bio.terra.common.tracing;

import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceParams;
import io.opencensus.trace.samplers.Samplers;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "terra.common.tracing")
public class TracingProperties implements InitializingBean {
  /** Rate of sampling, 0.0 - 1.0 */
  Double probability = 0.0;

  /** Whether Stackdriver tracing export is enabled at all */
  Boolean enabled = true;

  public Double getProbability() {
    return probability;
  }

  public void setProbability(Double probability) {
    this.probability = probability;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /** Propagates the workspace.tracing.probability property to the OpenCensus tracing config. */
  @Override
  public void afterPropertiesSet() {
    System.out.println("Probability: " + getProbability());
    TraceParams origParams = Tracing.getTraceConfig().getActiveTraceParams();
    Tracing.getTraceConfig()
        .updateActiveTraceParams(
            origParams
                .toBuilder()
                .setSampler(Samplers.probabilitySampler(getProbability()))
                .build());
  }
}
