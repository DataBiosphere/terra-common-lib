package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_VIEW_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_VIEW_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_LATENCY_VIEW_NAME;
import static bio.terra.common.stairway.test.MetricsTestUtil.ERROR_COUNT;
import static bio.terra.common.stairway.test.MetricsTestUtil.FATAL_COUNT;
import static bio.terra.common.stairway.test.MetricsTestUtil.assertCountIncremented;
import static bio.terra.common.stairway.test.MetricsTestUtil.assertLatencyCountIncremented;
import static bio.terra.common.stairway.test.MetricsTestUtil.getCurrentCount;
import static bio.terra.common.stairway.test.MetricsTestUtil.getCurrentDistributionDataCount;
import static bio.terra.common.stairway.test.MetricsTestUtil.sleepForSpansExport;

import bio.terra.stairway.FlightStatus;
import io.opencensus.tags.TagValue;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class MetricsHelperTest {
  private static final String FAKE_FLIGHT_NAME = "fakeFlight";
  private static final String FAKE_STEP_NAME = "fakeStep";

  private static final List<TagValue> FLIGHTS_LIST = List.of(TagValue.create(FAKE_FLIGHT_NAME));

  private static final List<TagValue> FLIGHT_STEP_LIST =
      List.of(TagValue.create(FAKE_FLIGHT_NAME), TagValue.create(FAKE_STEP_NAME));

  @Test
  void recordErrorCount() throws Exception {
    long fatalCount = getCurrentCount(FLIGHT_ERROR_VIEW_NAME, FATAL_COUNT);
    long errorCount = getCurrentCount(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT);

    MetricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.FATAL);
    MetricsHelper.recordFlightError(FAKE_FLIGHT_NAME, FlightStatus.SUCCESS);

    sleepForSpansExport();

    assertCountIncremented(FLIGHT_ERROR_VIEW_NAME, ERROR_COUNT, errorCount, 3);
    assertCountIncremented(FLIGHT_ERROR_VIEW_NAME, FATAL_COUNT, fatalCount, 1);
  }

  @Test
  void RecordFlightLatency() throws Exception {
    // this is mapped to the Distribution defined in MetricsHelper, i.e.
    // 0ms being within the first bucket & 1 ms in the 2nd.
    var zeroMsBucketIndex = 0;
    var oneMsBucketIndex = 1;

    long current0MsCount =
        getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, FLIGHTS_LIST, zeroMsBucketIndex);
    long current1MsCount =
        getCurrentDistributionDataCount(FLIGHT_LATENCY_VIEW_NAME, FLIGHTS_LIST, oneMsBucketIndex);

    MetricsHelper.recordFlightLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(1));
    MetricsHelper.recordFlightLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(1));
    MetricsHelper.recordFlightLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(0));

    sleepForSpansExport();

    // 1 ms,
    assertLatencyCountIncremented(
        FLIGHT_LATENCY_VIEW_NAME, FLIGHTS_LIST, current0MsCount, 1, zeroMsBucketIndex);
    // 2ms
    assertLatencyCountIncremented(
        FLIGHT_LATENCY_VIEW_NAME, FLIGHTS_LIST, current1MsCount, 2, oneMsBucketIndex);
  }

  @Test
  void recordStepLatency() throws Exception {
    // this is mapped to the Distribution defined in MetricsHelper, i.e.
    // 0ms being within the first bucket & 1 ms in the 2nd.
    var zeroMsBucketIndex = 0;
    var oneMsBucketIndex = 1;

    long current0MsCount =
        getCurrentDistributionDataCount(
            STEP_LATENCY_VIEW_NAME, FLIGHT_STEP_LIST, zeroMsBucketIndex);
    long current1MsCount =
        getCurrentDistributionDataCount(
            FLIGHT_LATENCY_VIEW_NAME, FLIGHT_STEP_LIST, oneMsBucketIndex);

    MetricsHelper.recordStepLatency(FAKE_FLIGHT_NAME, FAKE_STEP_NAME, Duration.ofMillis(1));
    MetricsHelper.recordStepLatency(FAKE_FLIGHT_NAME, FAKE_STEP_NAME, Duration.ofMillis(1));
    MetricsHelper.recordStepLatency(FAKE_FLIGHT_NAME, FAKE_STEP_NAME, Duration.ofMillis(0));

    sleepForSpansExport();

    // 1 ms,
    assertLatencyCountIncremented(
        STEP_LATENCY_VIEW_NAME, FLIGHT_STEP_LIST, current0MsCount, 1, zeroMsBucketIndex);
    // 2ms
    assertLatencyCountIncremented(
        STEP_LATENCY_VIEW_NAME, FLIGHT_STEP_LIST, current1MsCount, 2, oneMsBucketIndex);
  }
}
