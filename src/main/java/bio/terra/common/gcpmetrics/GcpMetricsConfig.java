package bio.terra.common.gcpmetrics;

import com.google.cloud.opentelemetry.metric.GoogleCloudMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "bio.terra.common.opentelemetry")
public class GcpMetricsConfig {
  private final Logger logger = LoggerFactory.getLogger(GcpMetricsConfig.class);

  /** Creates OpenTelemetry MetricReader that exports metrics to Google Cloud Monitoring */
  @Bean(destroyMethod = "close")
  @ConditionalOnProperty(
      name = "terra.common.google.metrics.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public MetricReader gcpMetricsReader() {
    logger.info("GCP metrics enabled.");
    return PeriodicMetricReader.create(GoogleCloudMetricExporter.createWithDefaultConfiguration());
  }
}
