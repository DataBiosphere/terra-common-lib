package bio.terra.common.metrics;

import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Objects;

/**
 * A Spring Component that exposes any OpenCensus metrics via a {@link HTTPServer}.
 */
@Component
public class MetricsComponent {
    private final Logger logger = LoggerFactory.getLogger(MetricsComponent.class);
    private final MetricsProperties metricsProperties;

    private HTTPServer prometheusServer;

    @Autowired
    public MetricsComponent(MetricsProperties metricsProperties) {
        this.metricsProperties = metricsProperties;
    }

    @PostConstruct
    private void startEndpointServer() {
        logger.info("Prometheus metrics endpoint enabled: {}", metricsProperties.isPrometheusEndpointEnabled());
        if (!metricsProperties.isPrometheusEndpointEnabled()) {
            return;
        }
        try {
            PrometheusStatsCollector.createAndRegister();
        } catch (IllegalArgumentException e) {
            logger.error("OpenCensus Prometheus Collector already registered.", e);
        }
        try {
            prometheusServer = new HTTPServer(metricsProperties.getPrometheusEndpointPort());
            logger.info("Prometheus endpoint server started. Port: {}", metricsProperties.getPrometheusEndpointPort());
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
