package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcdEventConsumerTest {

    @Mock
    private ServiceBusConfiguration serviceBusConfiguration;

    @Mock
    private CcdMessageProcessor processor;

    @Mock
    private ServiceBusSessionReceiverClient sessionReceiverClient;

    @Mock
    private ServiceBusReceiverClient receiverClient;

    @Mock
    private ServiceBusReceivedMessage messageStream;

    private CcdEventConsumer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdEventConsumer(serviceBusConfiguration, processor);
    }

    @Test
    void given_session_is_accepted_when_receiver_throws_error() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenThrow(IllegalStateException.class);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(0)).processMessage("testMessage");
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
        when(messageStream.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(messageStream);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));

        doNothing().when(processor).processMessage(any());

        doNothing().when(receiverClient).complete(messageStream);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(receiverClient, Mockito.times(1)).complete(messageStream);
    }

    @Test
    void given_session_is_accepted_when_invalid_message_consumed() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
        when(messageStream.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(messageStream);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));

        doThrow(JsonProcessingException.class).when(processor).processMessage(any());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(messageStream);
    }

}
