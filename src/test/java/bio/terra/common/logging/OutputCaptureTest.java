package bio.terra.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.system.OutputCaptureRule;

public class OutputCaptureTest {

  @Rule public OutputCaptureRule capture = new OutputCaptureRule();

  @Test
  public void testName() throws Exception {
    System.out.println("asdf");
    assertThat(capture).contains("asdf");
  }
}
