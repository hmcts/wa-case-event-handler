package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdCaseEventConsumerTest {

    @Mock
    private ServiceBusConfiguration serviceBusConfiguration;
    @Mock
    private ServiceBusSessionReceiverClient sessionReceiverClient;
    @Mock
    private ServiceBusReceiverClient receiverClient;
    @Mock
    private ServiceBusReceivedMessage receivedMessage;
    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;
    @Mock
    private EventMessageReceiverService eventMessageReceiverService;

    private CcdCaseEventsConsumer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdCaseEventsConsumer(serviceBusConfiguration, eventMessageReceiverService);
    }

    @Test
    void given_session_is_accepted_when_receiver_throws_error() {
        when(sessionReceiverClient.acceptNextSession()).thenThrow(IllegalStateException.class);

        underTest.consumeMessage(sessionReceiverClient);

        verify(receiverClient, Mockito.times(0)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_receiver_complete_throws_error() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doThrow(new ServiceBusException(new Exception(), ServiceBusErrorSource.UNKNOWN)).doNothing()
                .when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(sessionReceiverClient);

        verify(receiverClient, Mockito.times(2)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_receiver_complete_throws_error_on_both_calls() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doThrow(new ServiceBusException(new Exception(), ServiceBusErrorSource.UNKNOWN))
                .when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(sessionReceiverClient);

        verify(receiverClient, Mockito.times(2)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() {
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromString("TestMessage"));

        publishMessageToReceiver();

        doNothing().when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(sessionReceiverClient);

        verify(receiverClient, Mockito.times(1)).complete(receivedMessage);
    }

    private void publishMessageToReceiver() {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(receivedMessage);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));
    }

}
