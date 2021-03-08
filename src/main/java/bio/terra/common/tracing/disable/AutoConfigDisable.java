package bio.terra.common.tracing.disable;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Disable the io.opencensus.contrib.spring.autoconfig AutoConfiguration by default to allow clients
 * to opt-in to our own setup instead of needing to opt out when this library is added to their
 * classpath.
 *
 * <p>We include the io.opencensus.contrib.spring package to make use of its @Traced annotation. It
 * also, however, autoconfigures client and server trace annotations. We want to be able to make use
 * of the @Traced annotation, but client services importing this library should not be
 * autoconfigured. To do this, this sub package is EnableAutoConfiguratoin by
 * META-INF/spring.factories, but only uses tracing-disable.properties to disable the
 * io.opencensus.contrib.spring auto configuration.
 *
 * <p>Making this a subpackage of bio.terra.common.tracing allows us to EnableAutoConfiguration this
 * class without EnableAutoConfiguration the bio.terra.common.tracing package so that that package
 * is still opt-in.
 *
 * <p>This package does not disable or otherwise interact with the bio.terra.common.tracing package.
 */
@Configuration
@PropertySource("classpath:tracing-disable.properties")
class AutoConfigDisable {}
