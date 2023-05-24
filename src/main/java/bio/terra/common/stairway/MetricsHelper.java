package bio.terra.common.stairway;

import bio.terra.stairway.FlightStatus;
import com.google.common.collect.ImmutableList;
import io.opencensus.common.Scope;
import io.opencensus.stats.Aggregation;
import io.opencensus.stats.BucketBoundaries;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagMetadata;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.time.Duration;
import java.util.List;

public class MetricsHelper {
  public static final String METRICS_PREFIX = "terra/common-lib";
  public static final ViewManager viewManager = Stats.getViewManager();

  private static final Tagger tagger = Tags.getTagger();
  private static final TagMetadata tagMetadata =
      TagMetadata.create(TagMetadata.TagTtl.UNLIMITED_PROPAGATION);
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();
  private static final TagKey KEY_FLIGHT_NAME = TagKey.create("flight_name");
  private static final TagKey KEY_ERROR = TagKey.create("error_code");
  /** Unit string for count. */
  private static final String COUNT = "1";
  /** Unit string for millisecond. */
  private static final String MILLISECOND = "ms";
  /** {@link Measure} for latency in milliseconds. */
  private static final Measure.MeasureDouble LATENCY =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/latency", "Latency for stairway flight", MILLISECOND);
  /** {@link Measure} for number of errors from stairway flights. */
  private static final Measure.MeasureDouble ERROR_COUNT =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/error", "Number of stairway errors", COUNT);

  private static final Aggregation LATENCY_DISTRIBUTION =
      Aggregation.Distribution.create(
          BucketBoundaries.create(
              List.of(
                  0.0, 1.0, 2.0, 5.0, 10.0, 20.0, 40.0, 60.0, 80.0, 100.0, 120.0, 140.0, 160.0,
                  180.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0, 2000.0,
                  4000.0, 8000.0, 16000.0, 32000.0, 64000.0)));

  private static final Aggregation COUNT_AGGREGATION = Aggregation.Count.create();
  static final View.Name LATENCY_VIEW_NAME = View.Name.create(METRICS_PREFIX + "/stairway/latency");
  static final View.Name ERROR_VIEW_NAME = View.Name.create(METRICS_PREFIX + "/stairway/error");
  private static final View LATENCY_VIEW =
      View.create(
          LATENCY_VIEW_NAME,
          "The distribution of latencies",
          LATENCY,
          LATENCY_DISTRIBUTION,
          ImmutableList.of(KEY_FLIGHT_NAME));
  private static final View ERROR_VIEW =
      View.create(
          ERROR_VIEW_NAME,
          "The number and types of errors",
          ERROR_COUNT,
          COUNT_AGGREGATION,
          ImmutableList.of(KEY_ERROR, KEY_FLIGHT_NAME));
  private static final View[] views = new View[] {LATENCY_VIEW, ERROR_VIEW};

  // Register all views
  static {
    for (View view : views) {
      viewManager.registerView(view);
    }
  }

  /** Record the latency for stairway flights. */
  public static void recordLatency(String flightName, Duration latency) {
    TagContext tctx =
        tagger
            .emptyBuilder()
            .put(KEY_FLIGHT_NAME, TagValue.create(flightName), tagMetadata)
            .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(LATENCY, latency.toMillis()).record(tctx);
    }
  }

  /** Records the failed flights. */
  public static void recordError(String flightName, FlightStatus flightStatus) {
    if (FlightStatus.ERROR != flightStatus && FlightStatus.FATAL != flightStatus) {
      return;
    }
    TagContext tctx =
        tagger
            .emptyBuilder()
            .put(KEY_ERROR, TagValue.create(flightStatus.name()), tagMetadata)
            .put(KEY_FLIGHT_NAME, TagValue.create(flightName), tagMetadata)
            .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(ERROR_COUNT, 1).record(tctx);
    }
  }

  private MetricsHelper() {}
}
