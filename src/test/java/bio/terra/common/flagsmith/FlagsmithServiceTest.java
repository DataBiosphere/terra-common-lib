package bio.terra.common.flagsmith;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = FlagsmithServiceTestApplication.class)
@ActiveProfiles("flagsmith-test")
@Tag("unit")
public class FlagsmithServiceTest {

  @Autowired FlagsmithService flagsmithService;

  @Test
  public void isFeatureEnabled() throws Exception {
    assertTrue(flagsmithService.isFeatureEnabled("foo").get());
  }

  @Test
  public void isFeatureEnabled_featureUndefined_returnsDefaultValue() throws Exception {
    assertTrue(flagsmithService.isFeatureEnabled("bar").isEmpty());
  }
}
