package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdCaseEventDeadLetterQueueConsumerTest {

    @Mock
    private ServiceBusConfiguration serviceBusConfiguration;
    @Mock
    private ServiceBusReceiverClient receiverClient;
    @Mock
    private ServiceBusReceivedMessage receivedMessage;
    @Mock
    private EventMessageReceiverService eventMessageReceiverService;
    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    private CcdCaseEventsDeadLetterQueueConsumer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdCaseEventsDeadLetterQueueConsumer(serviceBusConfiguration, eventMessageReceiverService);
    }

    @Test
    void given_session_is_accepted_when_receiver_complete_throws_error() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doThrow(new ServiceBusException(new Exception(), ServiceBusErrorSource.UNKNOWN)).doNothing()
                .when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(receiverClient);

        verify(receiverClient, Mockito.times(1)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(1)).abandon(receivedMessage);
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_receiver_complete_and_abandon_throws_error_on_both_calls() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doThrow(new ServiceBusException(new Exception(), ServiceBusErrorSource.UNKNOWN)).doNothing()
            .when(receiverClient).complete(receivedMessage);
        doThrow(new ServiceBusException(new Exception(), ServiceBusErrorSource.UNKNOWN)).doNothing()
            .when(receiverClient).abandon(receivedMessage);

        underTest.consumeMessage(receiverClient);

        verify(receiverClient, Mockito.times(1)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(1)).abandon(receivedMessage);
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doNothing().when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(receiverClient);

        verify(receiverClient, Mockito.times(1)).complete(receivedMessage);
    }

    @Test
    void given_session_is_accepted_when_handling_message_throws_error() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doThrow(new RuntimeException()).when(eventMessageReceiverService).handleDlqMessage(any(), any());

        underTest.consumeMessage(receiverClient);

        verify(receiverClient, Mockito.times(1)).abandon(receivedMessage);
        verify(receiverClient, Mockito.times(0)).complete(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }


    @Test
    void should_start_consume_messages_when_consumer_start_is_called() {
        when(serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver()).thenReturn(receiverClient);
        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(Flux.empty()));

        Thread consumer = new Thread(underTest);
        consumer.start();

        await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> verify(receiverClient, atLeastOnce()).receiveMessages(1));
        underTest.stop();
    }

    @Test
    void should_stop_consume_messages_when_consumer_stop_is_called() {
        underTest.stop();
        Thread consumer = new Thread(underTest);
        consumer.start();

        verify(receiverClient, never()).receiveMessages(1);
    }

    private void publishMessageToReceiver() {
        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(receivedMessage);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));
    }

}
