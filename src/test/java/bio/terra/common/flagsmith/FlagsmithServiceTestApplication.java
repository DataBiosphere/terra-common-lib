package bio.terra.common.flagsmith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    },
    scanBasePackages = {"bio.terra.common.flagsmith"})
@EnableRetry
public class FlagsmithServiceTestApplication {

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(bio.terra.common.flagsmith.FlagsmithServiceTestApplication.class)
        .run(args);
  }
}
