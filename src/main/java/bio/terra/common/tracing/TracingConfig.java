package bio.terra.common.tracing;

import io.opencensus.trace.Tracing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

  // Enable the @Traced annotation for use within the application
  @Bean
  public CensusSpringAspect censusAspect() {
    return new CensusSpringAspect(Tracing.getTracer());
  }

}
