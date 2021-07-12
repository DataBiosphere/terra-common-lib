package bio.terra.common.retry.transaction;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.retry.annotation.EnableRetry;

/** A simple Spring Boot application for testing transaction retries. */
@SpringBootApplication(
    exclude = {
      // We don't make use of DataSource in this application, so exclude it from scanning.
      DataSourceAutoConfiguration.class,
    },
    scanBasePackages = {"bio.terra.common.retry.transaction"})
@EnableRetry
public class TransactionRetryTestApplication {

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(TransactionRetryTestApplication.class).run(args);
  }
}
