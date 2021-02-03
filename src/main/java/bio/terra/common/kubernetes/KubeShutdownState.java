package bio.terra.common.kubernetes;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Several components need to access the shutdown state. To avoid infinite dependency loops, we
 * factor the state into its own component.
 */
@Component
public class KubeShutdownState {
  private final AtomicBoolean isShutdown;

  public KubeShutdownState() {
    this.isShutdown = new AtomicBoolean(false);
  }

  public void clearShutdown() {
    isShutdown.set(false);
  }

  public void setShutdown() {
    isShutdown.set(true);
  }

  public boolean isShutdown() {
    return isShutdown.get();
  }
}
