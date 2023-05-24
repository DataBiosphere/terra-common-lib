package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.ERROR_VIEW_NAME;
import static bio.terra.common.stairway.MetricsHelper.LATENCY_VIEW_NAME;
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

  private static final List<TagValue> FLIGHTS_LIST = List.of(TagValue.create(FAKE_FLIGHT_NAME));

  @Test
  void testRecordErrorCount() throws Exception {
    long errorCount403 = getCurrentCount(ERROR_VIEW_NAME, FATAL_COUNT);
    long errorCount401 = getCurrentCount(ERROR_VIEW_NAME, ERROR_COUNT);

    MetricsHelper.recordError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordError(FAKE_FLIGHT_NAME, FlightStatus.ERROR);
    MetricsHelper.recordError(FAKE_FLIGHT_NAME, FlightStatus.FATAL);
    MetricsHelper.recordError(FAKE_FLIGHT_NAME, FlightStatus.SUCCESS);

    sleepForSpansExport();

    assertCountIncremented(ERROR_VIEW_NAME, ERROR_COUNT, errorCount401, 3);
    assertCountIncremented(ERROR_VIEW_NAME, FATAL_COUNT, errorCount403, 1);
  }

  @Test
  void testRecordLatency() throws Exception {
    // this is mapped to the Distribution defined in MetricsHelper, i.e.
    // 0ms being within the first bucket & 1 ms in the 2nd.
    var zeroMsBucketIndex = 0;
    var oneMsBucketIndex = 1;

    long current0MsCount =
        getCurrentDistributionDataCount(LATENCY_VIEW_NAME, FLIGHTS_LIST, zeroMsBucketIndex);
    long current1MsCount =
        getCurrentDistributionDataCount(LATENCY_VIEW_NAME, FLIGHTS_LIST, oneMsBucketIndex);

    MetricsHelper.recordLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(1));
    MetricsHelper.recordLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(1));
    MetricsHelper.recordLatency(FAKE_FLIGHT_NAME, Duration.ofMillis(0));

    sleepForSpansExport();

    // 1 ms,
    assertLatencyCountIncremented(
        LATENCY_VIEW_NAME, FLIGHTS_LIST, current0MsCount, 1, zeroMsBucketIndex);
    // 2ms
    assertLatencyCountIncremented(
        LATENCY_VIEW_NAME, FLIGHTS_LIST, current1MsCount, 2, oneMsBucketIndex);
  }
}
