package bio.terra.common.prometheus;

import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.prometheus.client.exporter.HTTPServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** A Spring Component that exposes any OpenCensus metrics via a Prometheus {@link HTTPServer}. */
@Component
public class PrometheusComponent {
  private static final Logger logger = LoggerFactory.getLogger(PrometheusComponent.class);
  private final PrometheusProperties prometheusProperties;

  private HTTPServer prometheusServer;

  @Autowired
  public PrometheusComponent(PrometheusProperties prometheusProperties) {
    this.prometheusProperties = prometheusProperties;
  }

  @PostConstruct
  private void startEndpointServer() {
    logger.info(
        "Prometheus metrics endpoint enabled: {}", prometheusProperties.isEndpointEnabled());
    if (!prometheusProperties.isEndpointEnabled()) {
      return;
    }
    try {
      PrometheusStatsCollector.createAndRegister();
    } catch (IllegalArgumentException e) {
      logger.error("OpenCensus Prometheus Collector already registered.", e);
    }
    try {
      prometheusServer = new HTTPServer(prometheusProperties.getEndpointPort());
      logger.info(
          "Prometheus endpoint server started. Port: {}", prometheusProperties.getEndpointPort());
    } catch (IOException e) {
      logger.error("Prometheus endpoint server error on startup.", e);
    }
  }

  @PreDestroy
  private void stopEndpointServer() {
    if (Objects.isNull(prometheusServer)) {
      return;
    }
    prometheusServer.close();
    logger.info("Prometheus server stopped.");
  }
}
