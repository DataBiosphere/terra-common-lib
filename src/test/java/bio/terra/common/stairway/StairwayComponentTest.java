package bio.terra.common.stairway;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.stairway.QueueInterface;
import bio.terra.stairway.azure.AzureServiceBusQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@ExtendWith(MockitoExtension.class)
class StairwayComponentTest {
  @InjectMocks private StairwayComponent stairwayComponent;

  @Mock private KubeProperties kubeProperties;
  private StairwayProperties stairwayProperties;
  @Mock private KubeService kubeService;
  @Mock private ThreadPoolTaskExecutor executor;

  @Test
  void setupAzureWorkQueueTest() {

    String connectionString =
        "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
    setProperties(connectionString, "topicName", "subscriptionName");

    stairwayComponent =
        new StairwayComponent(kubeService, kubeProperties, stairwayProperties, executor);
    QueueInterface queue = stairwayComponent.setupAzureWorkQueue();
    assertTrue(queue instanceof AzureServiceBusQueue);
  }

  @Test
  void setupAzureWorkQueueTestConnectionStringRequired() {
    setProperties("", "topicName", "subscriptionName");

    stairwayComponent =
        new StairwayComponent(kubeService, kubeProperties, stairwayProperties, executor);
    assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
  }

  @Test
  void setupAzureWorkQueueTestTopicNameRequired() {
    String connectionString =
        "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
    setProperties(connectionString, "", "subscriptionName");

    stairwayComponent =
        new StairwayComponent(kubeService, kubeProperties, stairwayProperties, executor);
    assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
  }

  @Test
  void setupAzureWorkQueueTestSubscriptionNameRequired() {
    String connectionString =
        "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
    setProperties(connectionString, "topicName", "");

    stairwayComponent =
        new StairwayComponent(kubeService, kubeProperties, stairwayProperties, executor);
    assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
  }

  @Test
  void setupAzureWorkQueueTestThrowsNPE() {
    stairwayComponent =
        new StairwayComponent(kubeService, kubeProperties, stairwayProperties, executor);
    assertThrows(NullPointerException.class, () -> stairwayComponent.setupAzureWorkQueue());
  }

  private void setProperties(String connectionString, String topicName, String subscription) {
    stairwayProperties = new StairwayProperties();
    stairwayProperties.setAzureServiceBusConnectionString(connectionString);
    stairwayProperties.setAzureServiceBusMaxAutoLockRenewDuration(1L);
    stairwayProperties.setAzureServiceBusNamespace("namespace");
    stairwayProperties.setAzureServiceBusTopicName(topicName);
    stairwayProperties.setAzureServiceBusSubscriptionName(subscription);
    stairwayProperties.setAzureQueueEnabled(true);
  }
}
