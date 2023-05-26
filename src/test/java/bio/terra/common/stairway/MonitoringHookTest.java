package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_VIEW_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_VIEW_NAME;
import static bio.terra.common.stairway.test.MetricsTestUtil.assertCountIncremented;
import static bio.terra.common.stairway.test.MetricsTestUtil.getCurrentCount;
import static bio.terra.common.stairway.test.MetricsTestUtil.getCurrentDistributionDataCount;
import static bio.terra.common.stairway.test.MetricsTestUtil.sleepForSpansExport;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import bio.terra.common.stairway.test.StairwayTestUtils;
import bio.terra.stairway.Flight;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.FlightMap;
import bio.terra.stairway.FlightState;
import bio.terra.stairway.FlightStatus;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.StairwayBuilder;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import bio.terra.stairway.StepStatus;
import io.opencensus.tags.TagValue;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracing;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class MonitoringHookTest {

  private static final List<TagValue> ERROR_COUNT_TAGS =
      List.of(
          TagValue.create(FlightStatus.ERROR.name()),
          TagValue.create(ErrorSpanRecordingFlight.class.getName()));

  @Test
  public void spansConnected() throws Exception {
    List<SpanContext> contextRecord = new ArrayList<>();
    RecordContextStep.setRecord(contextRecord);

    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, SpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));
    assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());

    // There should be 2 recorded Spans, one for each of the 2 steps.
    assertThat(contextRecord, Matchers.hasSize(2));
    assertSharedTraceId(contextRecord);
    assertAllDifferentSpanIds(contextRecord);
  }

  @Test
  void recordNoErrorOnSuccess() throws Exception {
    long errorCount = getCurrentCount(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT_TAGS);

    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, SpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());
    sleepForSpansExport();
    assertCountIncremented(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT_TAGS, errorCount, 0);
  }

  @Test
  void recordLatencyOnSuccess() throws Exception {
    var flightsList = List.of(TagValue.create(SpanRecordingFlight.class.getName()));
    List<Long> previousDistribution = new ArrayList<>();
    for (int i = 0; i < 29; i++) {
      previousDistribution.add(
          getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, flightsList, i));
    }

    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, SpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());
    sleepForSpansExport();
    int changedBucketCount = 0;
    for (int i = 0; i < 29; i++) {
      if (previousDistribution
          .get(i)
          .equals(getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, flightsList, i) - 1)) {
        changedBucketCount++;
      }
    }
    assertEquals(1, changedBucketCount);
  }

  /** A {@link Flight} with two steps for recording the span context. */
  public static class SpanRecordingFlight extends Flight {
    public SpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
      super(inputParameters, applicationContext);
      addStep(new RecordContextStep());
      addStep(new RecordContextStep());
    }
  }

  @Test
  public void undoSpansOnFailure() throws Exception {
    List<SpanContext> contextRecord = new ArrayList<>();
    RecordContextStep.setRecord(contextRecord);

    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));
    FlightState flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));
    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());
    // There are 4 recorded spans, 2 for each do and undo of the RecordContextStep execution.
    assertThat(contextRecord, Matchers.hasSize(4));
    assertSharedTraceId(contextRecord);
    assertAllDifferentSpanIds(contextRecord);
  }

  @Test
  void recordErrorOnFailure() throws Exception {
    long errorCount = getCurrentCount(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT_TAGS);
    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));

    var flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());
    sleepForSpansExport();
    assertCountIncremented(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT_TAGS, errorCount, 1);
  }

  @Test
  void recordLatencyOnFailure() throws Exception {
    var flightsList = List.of(TagValue.create(ErrorSpanRecordingFlight.class.getName()));
    List<Long> previousDistribution = new ArrayList<>();
    for (int i = 0; i < 29; i++) {
      previousDistribution.add(
          getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, flightsList, i));
    }
    Stairway stairway =
        StairwayTestUtils.setupStairway(new StairwayBuilder().stairwayHook(new MonitoringHook()));

    var flightState =
        StairwayTestUtils.blockUntilFlightCompletes(
            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));

    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());
    sleepForSpansExport();
    int changedBucketCount = 0;
    for (int i = 0; i < 29; i++) {
      if (previousDistribution
          .get(i)
          .equals(getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, flightsList, i) - 1)) {
        changedBucketCount++;
      }
    }
    assertEquals(1, changedBucketCount);
  }

  private static void assertSharedTraceId(List<SpanContext> contextRecord) {
    assertEquals(1, contextRecord.stream().map(SpanContext::getTraceId).distinct().count());
  }

  private static void assertAllDifferentSpanIds(List<SpanContext> contextRecord) {
    assertEquals(
        contextRecord.size(),
        contextRecord.stream().map(SpanContext::getSpanId).distinct().count());
  }

  /** A {@link Flight} with two steps for recording the span context and a {@link FailureStep}. */
  public static class ErrorSpanRecordingFlight extends Flight {
    public ErrorSpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
      super(inputParameters, applicationContext);
      addStep(new RecordContextStep());
      addStep(new RecordContextStep());
      addStep(new FailureStep());
    }
  }

  /** A {@link Step} that records the current span within each step execution. */
  public static class RecordContextStep implements Step {
    /** The current list within which to record step execution spans. */
    private static List<SpanContext> contextRecord;

    /** Set a new global list to append to for every {@link SpanContext} seen by these steps. */
    public static void setRecord(List<SpanContext> newRecord) {
      contextRecord = newRecord;
    }

    @Override
    public StepResult doStep(FlightContext flightContext) {
      record(Tracing.getTracer().getCurrentSpan().getContext());
      return StepResult.getStepResultSuccess();
    }

    @Override
    public StepResult undoStep(FlightContext flightContext) {
      record(Tracing.getTracer().getCurrentSpan().getContext());
      return StepResult.getStepResultSuccess();
    }

    private static void record(SpanContext context) {
      if (contextRecord != null) {
        contextRecord.add(context);
      }
    }
  }

  /** A {@link Step} that always fails. */
  public static class FailureStep implements Step {
    @Override
    public StepResult doStep(FlightContext flightContext) {
      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL);
    }

    @Override
    public StepResult undoStep(FlightContext flightContext) {
      return StepResult.getStepResultSuccess();
    }
  }
}
