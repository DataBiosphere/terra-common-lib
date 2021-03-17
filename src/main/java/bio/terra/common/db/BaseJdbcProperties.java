package bio.terra.common.db;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Common properties for all JDBC connections. Intended to be extended by a subclass annotated with
 * `@ConfigurationProperties(prefix = "my.pprefix")`
 */
// TODO: don't inherit these. Remove from common lib
public class BaseJdbcProperties {
  private String uri;
  private String username;
  private String password;

  public String getUri() {
    return uri;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  // NOTE: even though the setters appear unused, the Spring infrastructure uses them to populate
  // the properties.
  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.JSON_STYLE)
        .append("uri", uri)
        .append("username", username)
        .toString();
  }
}
