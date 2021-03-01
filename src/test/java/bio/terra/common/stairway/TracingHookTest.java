package bio.terra.common.stairway;

import org.junit.jupiter.api.Tag;

@Tag("unit")
public class TracingHookTest {
  //
  //  @Test
  //  public void spansConnected() throws Exception {
  //    List<SpanContext> contextRecord = new ArrayList<>();
  //    RecordContextStep.setRecord(contextRecord);
  //
  //    Stairway stairway =
  //        StairwayTestUtils.setupStairway(Stairway.newBuilder().stairwayHook(new TracingHook()));
  //    FlightState flightState =
  //        StairwayTestUtils.blockUntilFlightCompletes(
  //            stairway, SpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));
  //    assertEquals(FlightStatus.SUCCESS, flightState.getFlightStatus());
  //
  //    // There should be 2 recorded Spans, one for each of the 2 steps.
  //    assertThat(contextRecord, Matchers.hasSize(2));
  //    assertSharedTraceId(contextRecord);
  //    assertAllDifferentSpanIds(contextRecord);
  //  }
  //
  //  /** A {@link Flight} with two steps for recording the span context. */
  //  public static class SpanRecordingFlight extends Flight {
  //    public SpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
  //      super(inputParameters, applicationContext);
  //      addStep(new RecordContextStep());
  //      addStep(new RecordContextStep());
  //    }
  //  }
  //
  //  @Test
  //  public void undoSpansOnFailure() throws Exception {
  //    List<SpanContext> contextRecord = new ArrayList<>();
  //    RecordContextStep.setRecord(contextRecord);
  //
  //    Stairway stairway =
  //        StairwayTestUtils.setupStairway(Stairway.newBuilder().stairwayHook(new TracingHook()));
  //    FlightState flightState =
  //        StairwayTestUtils.blockUntilFlightCompletes(
  //            stairway, ErrorSpanRecordingFlight.class, new FlightMap(), Duration.ofSeconds(5));
  //    assertEquals(FlightStatus.ERROR, flightState.getFlightStatus());
  //    // There are 4 recorded spans, 2 for each do and undo of the RecordContextStep execution.
  //    assertThat(contextRecord, Matchers.hasSize(4));
  //    assertSharedTraceId(contextRecord);
  //    assertAllDifferentSpanIds(contextRecord);
  //  }
  //
  //  private static void assertSharedTraceId(List<SpanContext> contextRecord) {
  //    assertEquals(1, contextRecord.stream().map(SpanContext::getTraceId).distinct().count());
  //  }
  //
  //  private static void assertAllDifferentSpanIds(List<SpanContext> contextRecord) {
  //    assertEquals(
  //        contextRecord.size(),
  //        contextRecord.stream().map(SpanContext::getSpanId).distinct().count());
  //  }
  //
  //  /** A {@link Flight} with two steps for recording the span context and a {@link FailureStep}.
  // */
  //  public static class ErrorSpanRecordingFlight extends Flight {
  //    public ErrorSpanRecordingFlight(FlightMap inputParameters, Object applicationContext) {
  //      super(inputParameters, applicationContext);
  //      addStep(new RecordContextStep());
  //      addStep(new RecordContextStep());
  //      addStep(new FailureStep());
  //    }
  //  }
  //
  //  /** A {@link Step} that records the current span within each step execution. */
  //  public static class RecordContextStep implements Step {
  //    /** The current list within which to record step execution spans. */
  //    private static List<SpanContext> contextRecord;
  //
  //    /** Set a new global list to append to for every {@link SpanContext} seen by these steps. */
  //    public static void setRecord(List<SpanContext> newRecord) {
  //      contextRecord = newRecord;
  //    }
  //
  //    @Override
  //    public StepResult doStep(FlightContext flightContext) {
  //      record(Tracing.getTracer().getCurrentSpan().getContext());
  //      return StepResult.getStepResultSuccess();
  //    }
  //
  //    @Override
  //    public StepResult undoStep(FlightContext flightContext) {
  //      record(Tracing.getTracer().getCurrentSpan().getContext());
  //      return StepResult.getStepResultSuccess();
  //    }
  //
  //    private static void record(SpanContext context) {
  //      if (contextRecord != null) {
  //        contextRecord.add(context);
  //      }
  //    }
  //  }
  //
  //  /** A {@link Step} that always fails. */
  //  public static class FailureStep implements Step {
  //    @Override
  //    public StepResult doStep(FlightContext flightContext) {
  //      return new StepResult(StepStatus.STEP_RESULT_FAILURE_FATAL);
  //    }
  //
  //    @Override
  //    public StepResult undoStep(FlightContext flightContext) {
  //      return StepResult.getStepResultSuccess();
  //    }
  //  }
}
