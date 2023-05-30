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
  private static final TagKey KEY_FLIGHT_STATUS = TagKey.create("flight_status");
  private static final TagKey KEY_STEP_NAME = TagKey.create("step_name");
  private static final TagKey KEY_STEP_DIRECTION = TagKey.create("step_direction");
  private static final TagKey KEY_ERROR = TagKey.create("error_code");
  /** Unit string for count. */
  private static final String COUNT = "1";
  /** Unit string for millisecond. */
  private static final String MILLISECOND = "ms";
  /** {@link Measure} for latency in milliseconds. */
  private static final Measure.MeasureDouble FLIGHT_LATENCY =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/flight/latency", "Latency for stairway flight", MILLISECOND);
  /** {@link Measure} for number of errors from stairway flights. */
  private static final Measure.MeasureDouble FLIGHT_ERROR_COUNT =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/flight/error", "Number of stairway errors", COUNT);

  private static final Measure.MeasureDouble STEP_LATENCY =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/step/latency", "Latency for stairway step", MILLISECOND);
  /** {@link Measure} for number of errors from stairway steps. */
  private static final Measure.MeasureDouble STEP_ERROR_COUNT =
      Measure.MeasureDouble.create(
          METRICS_PREFIX + "/stairway/step/error", "Number of stairway step errors", COUNT);

  private static final Aggregation LATENCY_DISTRIBUTION =
      Aggregation.Distribution.create(
          BucketBoundaries.create(
              List.of(
                  0.0, 1.0, 2.0, 5.0, 10.0, 20.0, 40.0, 60.0, 80.0, 100.0, 120.0, 140.0, 160.0,
                  180.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0, 2000.0,
                  4000.0, 8000.0, 16000.0, 32000.0, 64000.0)));

  private static final Aggregation COUNT_AGGREGATION = Aggregation.Count.create();
  static final View.Name FLIGHT_LATENCY_VIEW_NAME =
      View.Name.create(METRICS_PREFIX + "/stairway/flight/latency");
  static final View.Name FLIGHT_ERROR_VIEW_NAME =
      View.Name.create(METRICS_PREFIX + "/stairway/flight/error");

  static final View.Name STEP_LATENCY_VIEW_NAME =
      View.Name.create(METRICS_PREFIX + "/stairway/step/latency");

  static final View.Name STEP_ERROR_VIEW_NAME =
      View.Name.create(METRICS_PREFIX + "/stairway/step/error");
  private static final View FLIGHT_LATENCY_VIEW =
      View.create(
          FLIGHT_LATENCY_VIEW_NAME,
          "The distribution of latencies",
          FLIGHT_LATENCY,
          LATENCY_DISTRIBUTION,
          ImmutableList.of(KEY_FLIGHT_NAME, KEY_FLIGHT_STATUS));
  private static final View FLIGHT_ERROR_VIEW =
      View.create(
          FLIGHT_ERROR_VIEW_NAME,
          "The number and types of errors",
          FLIGHT_ERROR_COUNT,
          COUNT_AGGREGATION,
          ImmutableList.of(KEY_ERROR, KEY_FLIGHT_NAME));

  private static final View STEP_LATENCY_VIEW =
      View.create(
          STEP_LATENCY_VIEW_NAME,
          "The distribution of latencies",
          STEP_LATENCY,
          LATENCY_DISTRIBUTION,
          ImmutableList.of(KEY_FLIGHT_NAME, KEY_STEP_DIRECTION, KEY_STEP_NAME));

  private static final View STEP_ERROR_VIEW =
      View.create(
          STEP_ERROR_VIEW_NAME,
          "The number of DO vs UNDO step",
          STEP_ERROR_COUNT,
          COUNT_AGGREGATION,
          ImmutableList.of(KEY_FLIGHT_NAME, KEY_STEP_DIRECTION, KEY_STEP_NAME));
  private static final View[] views =
      new View[] {FLIGHT_LATENCY_VIEW, FLIGHT_ERROR_VIEW, STEP_LATENCY_VIEW, STEP_ERROR_VIEW};

  // Register all views
  static {
    for (View view : views) {
      viewManager.registerView(view);
    }
  }

  /** Record the latency for stairway flights. */
  public static void recordFlightLatency(
      String flightName, FlightStatus flightStatus, Duration latency) {
    TagContext tctx =
        tagger
            .emptyBuilder()
            .put(KEY_FLIGHT_NAME, TagValue.create(flightName), tagMetadata)
            .put(KEY_FLIGHT_STATUS, TagValue.create(flightStatus.name()), tagMetadata)
            .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(FLIGHT_LATENCY, latency.toMillis()).record(tctx);
    }
  }

  /** Records the failed flights. */
  public static void recordFlightError(String flightName, FlightStatus flightStatus) {
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
      statsRecorder.newMeasureMap().put(FLIGHT_ERROR_COUNT, 1).record(tctx);
    }
  }

  /** Record the latency for stairway flights. */
  public static void recordStepLatency(
      String flightName, String stepDirection, String stepName, Duration latency) {
    TagContext tctx =
        tagger
            .emptyBuilder()
            .put(KEY_FLIGHT_NAME, TagValue.create(flightName), tagMetadata)
            .put(KEY_STEP_DIRECTION, TagValue.create(stepDirection), tagMetadata)
            .put(KEY_STEP_NAME, TagValue.create(stepName), tagMetadata)
            .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(STEP_LATENCY, latency.toMillis()).record(tctx);
    }
  }

  /** Records the failed flights. */
  public static void recordStepDirection(String flightName, String stepDirection, String stepName) {
    TagContext tctx =
        tagger
            .emptyBuilder()
            .put(KEY_FLIGHT_NAME, TagValue.create(flightName), tagMetadata)
            .put(KEY_STEP_DIRECTION, TagValue.create(stepDirection), tagMetadata)
            .put(KEY_STEP_NAME, TagValue.create(stepName), tagMetadata)
            .build();
    try (Scope ss = tagger.withTagContext(tctx)) {
      statsRecorder.newMeasureMap().put(STEP_ERROR_COUNT, 1).record(tctx);
    }
  }

  private MetricsHelper() {}
}
