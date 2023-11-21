package bio.terra.common.stairway;

import bio.terra.common.kubernetes.KubeProperties;
import bio.terra.common.kubernetes.KubeService;
import bio.terra.stairway.QueueInterface;
import bio.terra.stairway.Stairway;
import bio.terra.stairway.azure.AzureServiceBusQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(MockitoExtension.class)
class StairwayComponentTest {
    @InjectMocks
    private StairwayComponent stairwayComponent;
    @Mock
    private Stairway stairway;
    @Mock
    private KubeProperties kubeProperties;
    private StairwayProperties stairwayProperties;
    @Mock
    private KubeService kubeService;

    @Test
    public void setupAzureWorkQueueTest() {

        String connectionString = "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
        setProperties(connectionString, "topicName", "subscriptionName");

        stairwayComponent =
                new StairwayComponent(kubeService, kubeProperties, stairwayProperties, stairway);
        QueueInterface queue = stairwayComponent.setupAzureWorkQueue();
        assertTrue(queue instanceof AzureServiceBusQueue);
    }

    @Test
    public void setupAzureWorkQueueTestConnectionStringRequired() {
       setProperties("", "topicName", "subscriptionName");

        stairwayComponent =
                new StairwayComponent(kubeService, kubeProperties, stairwayProperties, stairway);
        assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
    }

    @Test
    public void setupAzureWorkQueueTestTopicNameRequired() {
        String connectionString = "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
        setProperties(connectionString, "", "subscriptionName");

        stairwayComponent =
                new StairwayComponent(kubeService, kubeProperties, stairwayProperties, stairway);
        assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
    }


    @Test
    public void setupAzureWorkQueueTestSubscriptionNameRequired() {
        String connectionString = "Endpoint=sb://azure-xxxx.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=xxxxx";
        setProperties(connectionString, "topicName", "");

        stairwayComponent =
                new StairwayComponent(kubeService, kubeProperties, stairwayProperties, stairway);
        assertThrows(IllegalArgumentException.class, () -> stairwayComponent.setupAzureWorkQueue());
    }

    @Test
    public void setupAzureWorkQueueTestThrowsNPE() {
        stairwayComponent =
                new StairwayComponent(kubeService, kubeProperties, stairwayProperties, stairway);
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