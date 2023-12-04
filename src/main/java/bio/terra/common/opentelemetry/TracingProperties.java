package bio.terra.common.opentelemetry;

import java.util.Collection;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "terra.common.tracing")
public record TracingProperties(double samplingRatio, Collection<String> excludedUrls) {}
