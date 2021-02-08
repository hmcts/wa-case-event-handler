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
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcdEventConsumerTest {

    @Mock
    private CcdMessageProcessor processor;

    @Mock
    private ServiceBusSessionReceiverClient sessionReceiverClient;

    @Mock
    private ServiceBusReceiverClient syncClient;

    @Mock
    private ServiceBusReceivedMessage messageStream;

    @Mock
    private IterableStream<ServiceBusReceivedMessage> iterableStream;

    private CcdEventConsumer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdEventConsumer(processor, "host",
                                         "topic", "subscription", 1);
    }

    @Test
    void given_session_is_accepted_when_receiver_thrown_error() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenThrow(IllegalStateException.class);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(0)).processMesssage("testMessage");
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(syncClient);
        when(messageStream.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(messageStream);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(syncClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));

        doNothing().when(processor).processMesssage(any());

        doNothing().when(syncClient).complete(messageStream);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(syncClient, Mockito.times(1)).complete(messageStream);
    }

    @Test
    void given_session_is_accepted_when_invalid_message_consumed() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(syncClient);
        when(messageStream.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(messageStream);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(syncClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));

        doThrow(JsonProcessingException.class).when(processor).processMesssage(any());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(syncClient, Mockito.times(0)).complete(messageStream);
    }

}
