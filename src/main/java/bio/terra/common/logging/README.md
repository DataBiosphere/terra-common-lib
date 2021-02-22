# Terra Common Logging

Terra services are configured to send their log output to `stdout` and `stderr`.
The host Kubernetes platform (GKE in Google Cloud) is responsible for ingesting
and relaying log data for storage and analysis by operators.

When running on Google Cloud, JSON-formatted logs are preferred over plain text. 
Google's `fluentd` agent, which is loaded in the GKE operating environment, is
able to parse JSON logs in a variety of ways, enriching the logging payload
before it gets relayed to Cloud Logging for storage. See 
[Structured Logging](https://cloud.google.com/logging/docs/structured-logging#special-payload-fields) docs
and the [GoogleJsonLayout.java](GoogleJsonLayout.java) file for more details.

## Quickstart

Below is typical logging config boilerplate for a Terra application. In order to
initialize the logging config as early as possible, it is  recommended to add 
the initializer directly to the Spring application builder:

```
@SpringBootApplication(scanBasPackages = {"bio.terra.common.logging", ...})
public class MyApp {
  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(MyApp.class)
        .initializers(new LoggingInitializer())
        .run(args);
  }
```

### Human-readable logging

To support local development, JSON-formatted logging will be turned off when the
`human-readable-logging` Spring profile is active.

You can add this into a Gradle task and/or include it on the command-line:

```
# Command-line arguments
./gradlew bootRun --args='--spring.profiles.active=human-readable-logging'

# Gradle task wrapper
task bootRunDev {
    bootRun.configure {
        systemProperty "spring.profiles.active", 'human-readable-logging'
    }
}
bootRunDev.finalizedBy bootRun
```

## Structured logs

To log structured data to Cloud Logging:

```
log.info("My message", LoggingUtils.jsonFromString("{eventType: 'very-rare-event'}"));

log.info("My message", LoggingUtils.structuredLogData("event", myEventObject));
```

See [LoggingUtils](LoggingUtils.java) Javadoc for more details.