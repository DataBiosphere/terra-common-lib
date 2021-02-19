package bio.terra.common.logging;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** A simple Spring Boot application for testing common logging utilities. */
@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    })
public class LoggingTestApplication {

  // N.B. this main method isn't called by the test! This is a demonstration of how a Terra
  // application might initialize the logging setup at the very beginning of the Spring Boot flow.
  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(LoggingTestApplication.class)
        .initializers(new LoggingInitializer())
        .run(args);
  }
}
