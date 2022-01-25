package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterQueuePeekServiceTest {

    @Mock
    private ServiceBusConfiguration serviceBusConfiguration;

    @Mock
    private ServiceBusReceiverAsyncClient serviceBusReceiverAsyncClient;

    @Mock
    private Mono<ServiceBusReceivedMessage> serviceBusReceivedMessageMono;

    @Mock
    private ServiceBusReceivedMessage serviceBusReceivedMessage;

    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @BeforeEach
    void setup() {
        when(serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueAsyncClient())
                .thenReturn(serviceBusReceiverAsyncClient);
        deadLetterQueuePeekService = new DeadLetterQueuePeekService(serviceBusConfiguration);
    }

    @Test
    void should_return_true_when_dlq_is_empty() {
        when(serviceBusReceivedMessageMono.block()).thenReturn(null);
        when(serviceBusReceiverAsyncClient.peekMessage(1)).thenReturn(serviceBusReceivedMessageMono);

        assertTrue(deadLetterQueuePeekService.isDeadLetterQueueEmpty());
    }

    @Test
    void should_return_false_when_dlq_is_not_empty() {
        when(serviceBusReceivedMessageMono.block()).thenReturn(serviceBusReceivedMessage);
        when(serviceBusReceiverAsyncClient.peekMessage(1)).thenReturn(serviceBusReceivedMessageMono);

        assertFalse(deadLetterQueuePeekService.isDeadLetterQueueEmpty());
    }
}