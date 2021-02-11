package bio.terra.common.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/** A simple Spring Boot application for testing common logging utilities. */
@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    },
    scanBasePackages = {"bio.terra.common.logging"})
public class LoggingTestApplication {

  // N.B. this main method isn't called by the test! This is a demonstration of how a Terra
  // application might initialize the logging setup at the very beginning of the Spring Boot flow.
  public static void main(String[] args) throws Exception {
    SpringApplication app = new SpringApplication(LoggingTestApplication.class);
    app.addInitializers(new LoggingInitializer());
    app.run(args);
  }
}
