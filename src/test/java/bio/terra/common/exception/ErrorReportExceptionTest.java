package bio.terra.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Test for {@link ErrorReportException} */
@Tag("unit")
public class ErrorReportExceptionTest {
  @Test
  public void encodeMessage() {
    assertEquals("test", new UnauthorizedException("test").getMessage());
    assertEquals("&lt;test&gt;", new UnauthorizedException("<test>").getMessage());
  }
}
