package bio.terra.common.flagsmith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
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
  @Autowired ObjectMapper objectMapper;

  @Test
  public void isFeatureEnabled() {
    assertTrue(flagsmithService.isFeatureEnabled("foo").get());
  }

  @Test
  public void isFeatureEnabled_userDisabled() {
    // even though feature foo is enabled, it is disabled for foo@bar.com user.
    assertFalse(flagsmithService.isFeatureEnabled("foo", "foo@bar.com").get());
  }

  @Test
  public void getFeatureValue() {
    FooValue fooValue = flagsmithService.getFeatureValueJson("foo", FooValue.class).get();

    assertEquals("world", fooValue.hello);
    assertEquals("world2", fooValue.hello2);
  }

  @Test
  public void getFeatureValue_featureHasNoValue() {
    assertTrue(flagsmithService.isFeatureEnabled("foo_no_value").get());
    assertTrue(flagsmithService.getFeatureValueJson("foo_no_value", Void.class).isEmpty());
  }

  @Test
  public void getFeatureValue_featureHasValueForUserFooBar() {
    assertTrue(flagsmithService.isFeatureEnabled("foo_no_value", "foo@bar.com").get());
    FooValue fooValue =
        flagsmithService.getFeatureValueJson("foo_no_value", FooValue.class, "foo@bar.com").get();

    // Though foo_no_value by default has no value, it has value for foo@bar.com user.
    assertEquals("world", fooValue.hello);
  }

  @Test
  public void isFeatureEnabled_featureUndefined_returnsDefaultValue() {
    assertTrue(flagsmithService.isFeatureEnabled("bar").isEmpty());
  }

  @Test
  public void getFeatureValue_featureUndefined_returnsDefaultValue() {
    assertTrue(flagsmithService.getFeatureValueJson("bar", Void.class).isEmpty());
  }

  private record FooValue(String hello, String hello2) {}
}
