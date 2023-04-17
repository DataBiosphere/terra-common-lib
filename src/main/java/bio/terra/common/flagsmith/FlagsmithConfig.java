package bio.terra.common.flagsmith;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(value = {FlagsmithProperties.class})
public class FlagsmithConfig {}
