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

To support local development, JSON-formatted logging will be turned off when the
`human-readable-logging` Spring profile is active. 

## Quickstart

Typical logging setup for a Terra Spring application:

```
// Scan the logging package for annotated components
@SpringBootApplication(scanBasePackages = {"bio.terra.common.logging", ...})
public class MyApp {
  public static void main(String[] args) throws Exception {
    SpringApplication app = new SpringApplication(MyApp.class);
    // Insert the logging initializer into the app startup chain.
    app.addInitializers(new LoggingInitializer());
    app.run(args);
  }
}
```

To log structured data to Cloud Logging:

```
log.info("My message", LoggingUtils.jsonFromString("{eventType: 'very-rare-event'}"));

log.info("My message", LoggingUtils.structuredLogData("event", myEventObject));
```