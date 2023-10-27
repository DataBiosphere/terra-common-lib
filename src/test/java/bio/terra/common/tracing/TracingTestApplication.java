package bio.terra.common.tracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/** A simple Spring Boot application for testing common tracing utilities. */
@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    },
    // Scan the common tracing package to pick up tracing auto configuration. Scan the logging
    // package to get request ID setting.
    scanBasePackages = {"bio.terra.common.logging"})
public class TracingTestApplication {
  // N.B. this main method isn't called by the test! This is a demonstration of how a Terra
  // application might initialize the tracing setup at the very beginning of the Spring Boot flow.
  public static void main(String[] args) throws Exception {
    new SpringApplication(TracingTestApplication.class).run(args);
  }
}
