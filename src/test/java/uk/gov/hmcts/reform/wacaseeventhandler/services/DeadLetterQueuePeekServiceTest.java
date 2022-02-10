package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueuePeekServiceTest {

    @Mock
    private ServiceBusConfiguration serviceBusConfiguration;

    @Mock
    private ServiceBusReceiverClient serviceBusReceiverClient;


    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @Test
    void should_return_true_when_dlq_is_empty() {

        when(serviceBusReceiverClient.peekMessage(1)).thenReturn(null);
        when(serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver())
                .thenReturn(serviceBusReceiverClient);

        deadLetterQueuePeekService = new DeadLetterQueuePeekService(serviceBusConfiguration);

        assertTrue(deadLetterQueuePeekService.isDeadLetterQueueEmpty());
    }

    @Test
    void should_return_false_when_dlq_is_not_empty() {

        ServiceBusReceivedMessage msg = Mockito.mock(ServiceBusReceivedMessage.class);
        when(serviceBusReceiverClient.peekMessage(1)).thenReturn(msg);
        when(serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver())
                .thenReturn(serviceBusReceiverClient);

        deadLetterQueuePeekService = new DeadLetterQueuePeekService(serviceBusConfiguration);

        assertFalse(deadLetterQueuePeekService.isDeadLetterQueueEmpty());
    }
}
