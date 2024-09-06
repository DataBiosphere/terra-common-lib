/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package bio.terra.common.opentelemetry;

import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static java.util.Arrays.asList;

import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.List;
import java.util.stream.DoubleStream;

/**
 * copied from <a
 * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/release/v2.0.x/instrumentation-api/src/main/java/io/opentelemetry/instrumentation/api/semconv/http/HttpMetricsAdvice.java">HttpMetricsAdvice</a>
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
                HttpAttributes.HTTP_REQUEST_METHOD,
                HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                ERROR_TYPE,
                NetworkAttributes.NETWORK_PROTOCOL_NAME,
                NetworkAttributes.NETWORK_PROTOCOL_VERSION,
                ServerAttributes.SERVER_ADDRESS,
                ServerAttributes.SERVER_PORT));
  }

  static void applyServerDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }
    ((ExtendedDoubleHistogramBuilder) builder)
        .setAttributesAdvice(
            asList(
                HttpAttributes.HTTP_ROUTE,
                HttpAttributes.HTTP_REQUEST_METHOD,
                HttpAttributes.HTTP_RESPONSE_STATUS_CODE,
                ERROR_TYPE,
                NetworkAttributes.NETWORK_PROTOCOL_NAME,
                NetworkAttributes.NETWORK_PROTOCOL_VERSION,
                UrlAttributes.URL_SCHEME));
  }

  private HttpMetricsAdvice() {}
}
