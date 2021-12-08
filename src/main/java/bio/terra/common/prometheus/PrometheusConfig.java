package bio.terra.common.prometheus;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {PrometheusProperties.class})
public class PrometheusConfig {}
