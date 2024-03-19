/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package bio.terra.common.opentelemetry;

import static java.util.Arrays.asList;

import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.extension.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * copied from <a
 * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/release/v2.2.x/instrumentation-api/src/main/java/io/opentelemetry/instrumentation/api/semconv/http/HttpMetricsAdvice.java">HttpMetricsAdvice</a>
 * for the only purpose of overriding the DURATION_SECONDS_BUCKETS
 */
final class HttpMetricsAdvice {

  // every 10ms up to 100ms, then every 190ms up to 2s (190 ends neatly at 2s), then every 2s up to
  // 10s, then every 30s up to 3m
  static final List<Double> DURATION_SECONDS_BUCKETS =
      // does all the math in terms of milliseconds, then converts to seconds, otherwise the
      // precision is wonky
      DoubleStream.iterate(
              10,
              d -> d < 60000 * 3,
              d -> {
                if (d < 100) {
                  return d + 10;
                }
                if (d < 2000) {
                  return d + 190;
                }
                if (d < 10000) {
                  return d + 2000;
                }
                return d + 20000;
              })
          .map(d -> d / 1000.0)
          .boxed()
          .toList();

  static void applyClientDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }
    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            asList(
                SemanticAttributes.HTTP_REQUEST_METHOD,
                SemanticAttributes.HTTP_RESPONSE_STATUS_CODE,
                SemanticAttributes.ERROR_TYPE,
                SemanticAttributes.NETWORK_PROTOCOL_NAME,
                SemanticAttributes.NETWORK_PROTOCOL_VERSION,
                SemanticAttributes.SERVER_ADDRESS,
                SemanticAttributes.SERVER_PORT));
  }

  static void applyServerDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }
    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            asList(
                SemanticAttributes.HTTP_ROUTE,
                SemanticAttributes.HTTP_REQUEST_METHOD,
                SemanticAttributes.HTTP_RESPONSE_STATUS_CODE,
                SemanticAttributes.ERROR_TYPE,
                SemanticAttributes.NETWORK_PROTOCOL_NAME,
                SemanticAttributes.NETWORK_PROTOCOL_VERSION,
                SemanticAttributes.URL_SCHEME));
  }

  private HttpMetricsAdvice() {}
}
