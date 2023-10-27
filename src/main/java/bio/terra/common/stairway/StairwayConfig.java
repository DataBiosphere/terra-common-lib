package bio.terra.common.stairway;

import static bio.terra.common.stairway.MetricsHelper.COUNT_AGGREGATION;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.FLIGHT_LATENCY_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.KEY_ERROR;
import static bio.terra.common.stairway.MetricsHelper.KEY_FLIGHT_NAME;
import static bio.terra.common.stairway.MetricsHelper.KEY_FLIGHT_STATUS;
import static bio.terra.common.stairway.MetricsHelper.KEY_STEP_DIRECTION;
import static bio.terra.common.stairway.MetricsHelper.KEY_STEP_NAME;
import static bio.terra.common.stairway.MetricsHelper.LATENCY_DISTRIBUTION;
import static bio.terra.common.stairway.MetricsHelper.STEP_ERROR_METER_NAME;
import static bio.terra.common.stairway.MetricsHelper.STEP_LATENCY_METER_NAME;

import io.opentelemetry.sdk.metrics.InstrumentSelector;
import io.opentelemetry.sdk.metrics.View;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

@Configuration
@EnableConfigurationProperties(value = {StairwayProperties.class})
public class StairwayConfig {
  @Bean(name = FLIGHT_LATENCY_METER_NAME)
  public Pair<InstrumentSelector, View> flightLatencyView() {
    return Pair.of(
        InstrumentSelector.builder().setMeterName(FLIGHT_LATENCY_METER_NAME).build(),
        View.builder()
            .setName(FLIGHT_LATENCY_METER_NAME)
            .setDescription("The distribution of latencies")
            .setAggregation(LATENCY_DISTRIBUTION)
            .setAttributeFilter(Set.of(KEY_FLIGHT_NAME.getKey(), KEY_FLIGHT_STATUS.getKey()))
            .build());
  }

  @Bean(name = FLIGHT_ERROR_METER_NAME)
  public Pair<InstrumentSelector, View> flightErrorView() {
    return Pair.of(
        InstrumentSelector.builder().setMeterName(FLIGHT_ERROR_METER_NAME).build(),
        View.builder()
            .setName(FLIGHT_ERROR_METER_NAME)
            .setDescription("The number and types of errors")
            .setAggregation(COUNT_AGGREGATION)
            .setAttributeFilter(Set.of(KEY_FLIGHT_NAME.getKey(), KEY_ERROR.getKey()))
            .build());
  }

  @Bean(name = STEP_LATENCY_METER_NAME)
  public Pair<InstrumentSelector, View> stepLatencyView() {
    return Pair.of(
        InstrumentSelector.builder().setMeterName(STEP_LATENCY_METER_NAME).build(),
        View.builder()
            .setName(STEP_LATENCY_METER_NAME)
            .setDescription("The distribution of latencies")
            .setAggregation(LATENCY_DISTRIBUTION)
            .setAttributeFilter(
                Set.of(
                    KEY_FLIGHT_NAME.getKey(), KEY_STEP_DIRECTION.getKey(), KEY_STEP_NAME.getKey()))
            .build());
  }

  @Bean(name = STEP_ERROR_METER_NAME)
  public Pair<InstrumentSelector, View> stepErrorView() {
    return Pair.of(
        InstrumentSelector.builder().setMeterName(STEP_ERROR_METER_NAME).build(),
        View.builder()
            .setName(STEP_ERROR_METER_NAME)
            .setDescription("The number and types of errors")
            .setAggregation(COUNT_AGGREGATION)
            .setAttributeFilter(
                Set.of(
                    KEY_FLIGHT_NAME.getKey(), KEY_STEP_DIRECTION.getKey(), KEY_STEP_NAME.getKey()))
            .build());
  }
}
