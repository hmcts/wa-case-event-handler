package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.amqp.models.AmqpAnnotatedMessage;
import com.azure.core.amqp.models.AmqpMessageHeader;
import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
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
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CcdEventException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.DeadLetterService;

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
    private CcdEventProcessor processor;
    @Mock
    private ServiceBusSessionReceiverClient sessionReceiverClient;
    @Mock
    private ServiceBusReceiverClient receiverClient;
    @Mock
    private ServiceBusReceivedMessage messageStream;
    @Mock
    private AmqpAnnotatedMessage amqpAnnotatedMessage;
    @Mock
    private AmqpMessageHeader header;
    @Mock
    private DeadLetterService deadLetterService;

    private CcdEventConsumer underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdEventConsumer(serviceBusConfiguration, processor, deadLetterService, 0);
    }

    @Test
    void given_session_is_accepted_when_receiver_throws_error() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenThrow(IllegalStateException.class);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(0)).processMesssage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(messageStream);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() throws IOException {
        attachMessageToReceiver();

        doNothing().when(processor).processMesssage(any());

        doNothing().when(receiverClient).complete(messageStream);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(receiverClient, Mockito.times(1)).complete(messageStream);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_invalid_message_consumed() throws IOException {
        attachMessageToReceiver();

        doThrow(JsonProcessingException.class).when(processor).processMesssage(any());

        when(deadLetterService.handleParsingError(any(), any())).thenReturn(new DeadLetterOptions());
        doNothing().when(receiverClient).deadLetter(any(), any());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(messageStream);
        verify(receiverClient, Mockito.times(1)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_exception_thrown_from_downstream_on_first_attempt() throws IOException {
        underTest = new CcdEventConsumer(serviceBusConfiguration, processor, deadLetterService, 3);

        attachMessageToReceiver();

        when(messageStream.getRawAmqpMessage()).thenReturn(amqpAnnotatedMessage);
        when(amqpAnnotatedMessage.getHeader()).thenReturn(header);
        when(header.getDeliveryCount()).thenReturn(1L);

        doThrow(CcdEventException.class).when(processor).processMesssage(any());

        doNothing().when(receiverClient).abandon(any());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(messageStream);
        verify(receiverClient, Mockito.times(1)).abandon(any());
    }

    @Test
    void given_session_is_accepted_when_exception_thrown_from_downstream_after_max_attempts() throws IOException {
        attachMessageToReceiver();
        header.setDeliveryCount(Long.valueOf(1));

        when(messageStream.getRawAmqpMessage()).thenReturn(amqpAnnotatedMessage);
        when(amqpAnnotatedMessage.getHeader()).thenReturn(header);
        when(header.getDeliveryCount()).thenReturn(3L);

        when(deadLetterService.handleApplicationError(any(), any())).thenReturn(new DeadLetterOptions());

        doThrow(CcdEventException.class).when(processor).processMesssage(any());

        doNothing().when(receiverClient).deadLetter(any(), any());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMesssage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(messageStream);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(1)).deadLetter(any(), any());
    }

    private void attachMessageToReceiver() {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
        when(messageStream.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(messageStream);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));
    }

}
