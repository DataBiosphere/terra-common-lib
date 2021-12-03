package bio.terra.common.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for enabling service metrics export to Prometheus.
 */
@ConfigurationProperties(prefix = "terra.common.metrics")
public class MetricsProperties {
    /**
     * If the endpoint--more specifically, the server backing it--should be started.
     *
     * <p>This is true by default because a service using this library must already scan this
     * package specifically to pick up the endpoint component, and flagging the endpoint for
     * scraping is a separate external toggle (e.g. ServiceMonitor resources in Kubernetes).
     */
    private boolean prometheusEndpointEnabled = true;

    /**
     * The port that the endpoint server should run on, if enabled. Must be different from the
     * service's normal operation port.
     *
     * <p>Various parts of Prometheus itself use 9090-9094 by default, off-the-shelf exporters
     * usually use 9100-9999.
     * <a href="https://github.com/prometheus/prometheus/wiki/Default-port-allocations">Docs.</a>
     */
    private int prometheusEndpointPort = 9098;


    public boolean isPrometheusEndpointEnabled() {
        return prometheusEndpointEnabled;
    }

    public void setPrometheusEndpointEnabled(boolean prometheusEndpointEnabled) {
        this.prometheusEndpointEnabled = prometheusEndpointEnabled;
    }

    public int getPrometheusEndpointPort() {
        return prometheusEndpointPort;
    }

    public void setPrometheusEndpointPort(int prometheusEndpointPort) {
        this.prometheusEndpointPort = prometheusEndpointPort;
    }
}
