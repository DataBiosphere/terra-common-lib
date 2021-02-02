package bio.terra.common.logging;

import com.google.auto.value.AutoValue;

/** */
@AutoValue
public abstract class ErrorReportingServiceContext {
  public abstract String service();

  public abstract String version();

  public static ErrorReportingServiceContext create(String service, String version) {
    return new AutoValue_ErrorReportingServiceContext(service, version);
  }
}
