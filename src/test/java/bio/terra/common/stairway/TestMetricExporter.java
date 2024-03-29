package bio.terra.common.stairway;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.List;

/** A test metric exporter that stores the last metrics it received. */
public class TestMetricExporter implements MetricExporter {
  private Collection<MetricData> lastMetrics;

  @Override
  public synchronized CompletableResultCode export(Collection<MetricData> metrics) {
    lastMetrics = List.copyOf(metrics);
    return CompletableResultCode.ofSuccess();
  }

  public synchronized Collection<MetricData> getLastMetrics() {
    return lastMetrics;
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }
}
