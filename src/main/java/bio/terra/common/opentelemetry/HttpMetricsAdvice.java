/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package bio.terra.common.opentelemetry;

import static java.util.Arrays.asList;

import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.extension.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.internal.HttpAttributes;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * copied from <a
 * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/release/v2.0.x/instrumentation-api/src/main/java/io/opentelemetry/instrumentation/api/semconv/http/HttpMetricsAdvice.java">HttpMetricsAdvice</a>
 * for the only purpose of overriding the DURATION_SECONDS_BUCKETS
 */
final class HttpMetricsAdvice {

  // every 10ms up to 500ms, then every 100ms up to 2s, then every 1s up to 10s, then every 5s up to
  // 1m, then every 10s up to 3m
  static final List<Double> DURATION_SECONDS_BUCKETS =
      DoubleStream.iterate(
              0.01,
              d -> d < (double) 60 * 3,
              d -> {
                if (d < .5) {
                  return d + 0.01;
                }
                if (d < 2) {
                  return d + 0.1;
                }
                if (d < 10) {
                  return d + 1;
                }
                if (d < 60) {
                  return d + 5;
                }
                return d + 10;
              })
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
                HttpAttributes.ERROR_TYPE,
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
                HttpAttributes.ERROR_TYPE,
                SemanticAttributes.NETWORK_PROTOCOL_NAME,
                SemanticAttributes.NETWORK_PROTOCOL_VERSION,
                SemanticAttributes.URL_SCHEME));
  }

  private HttpMetricsAdvice() {}
}
