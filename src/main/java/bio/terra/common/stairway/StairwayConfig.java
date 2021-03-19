package bio.terra.common.stairway;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {StairwayProperties.class})
public class StairwayConfig {}
