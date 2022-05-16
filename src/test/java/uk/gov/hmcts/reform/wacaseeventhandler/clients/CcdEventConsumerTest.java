package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventErrorHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;

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
    private ServiceBusReceivedMessage receivedMessage;
    @Mock
    private CcdEventErrorHandler ccdEventErrorHandler;
    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @InjectMocks
    private CcdEventConsumer underTest;

    @Test
    void given_session_is_accepted_when_receiver_throws_error() throws IOException {
        when(sessionReceiverClient.acceptNextSession()).thenThrow(IllegalStateException.class);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(0)).processMessage("testMessage");
        verify(receiverClient, Mockito.times(0)).complete(receivedMessage);
        verify(receiverClient, Mockito.times(0)).abandon(any());
        verify(receiverClient, Mockito.times(0)).deadLetter(any(), any());
    }

    @Test
    void given_session_is_accepted_when_message_is_consumed() throws IOException {
        publishMessageToReceiver();

        doNothing().when(processor).processMessage(anyString());

        doNothing().when(receiverClient).complete(receivedMessage);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(receiverClient, Mockito.times(1)).complete(receivedMessage);
    }

    @Test
    void given_session_is_accepted_when_invalid_message_consumed() throws IOException {
        publishMessageToReceiver();

        doThrow(JsonProcessingException.class).when(processor).processMessage(anyString());

        doNothing().when(ccdEventErrorHandler).handleJsonError(any(), any(), any());
        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(ccdEventErrorHandler, Mockito.times(1))
            .handleJsonError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleApplicationError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleGenericError(any(), any(), any());
    }

    @Test
    void given_session_is_accepted_when_exception_thrown_from_downstream() throws IOException {
        publishMessageToReceiver();

        doThrow(RestClientException.class).when(processor).processMessage(anyString());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleJsonError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(1))
            .handleApplicationError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleGenericError(any(), any(), any());
    }

    @Test
    void given_session_is_accepted_when_unknown_exception_thrown_from_application() throws IOException {
        publishMessageToReceiver();

        doThrow(NullPointerException.class).when(processor).processMessage(anyString());

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(1)).processMessage("testMessage");
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleJsonError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(0))
            .handleApplicationError(any(), any(), any());
        verify(ccdEventErrorHandler, Mockito.times(1))
            .handleGenericError(any(), any(), any());
    }

    @Test
    void given_dlq_db_insert_flag_is_true_when_message_is_received_should_not_process_message() throws IOException {
        publishMessageToReceiver();

        when(featureFlagProvider.getBooleanValue(eq(DLQ_DB_INSERT), any())).thenReturn(true);

        underTest.consumeMessage(sessionReceiverClient);

        verify(processor, Mockito.times(0)).processMessage("testMessage");
    }


    @Test
    void should_start_consume_messages_when_consumer_start_is_called() {
        when(serviceBusConfiguration.createSessionReceiver()).thenReturn(sessionReceiverClient);
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(Flux.empty()));

        Thread consumer = new Thread(underTest);
        consumer.start();

        await()
            .atMost(1, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                verify(sessionReceiverClient, atLeastOnce()).acceptNextSession();
                verify(receiverClient, atLeastOnce()).receiveMessages(1);
            });
        underTest.stop();
    }

    @Test
    void should_stop_consume_messages_when_consumer_stop_is_called() {
        underTest.stop();
        Thread consumer = new Thread(underTest);
        consumer.start();

        verify(sessionReceiverClient, never()).acceptNextSession();
        verify(receiverClient, never()).receiveMessages(1);
    }

    private void publishMessageToReceiver() {
        when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
        when(receivedMessage.getBody()).thenReturn(BinaryData.fromBytes("testMessage".getBytes()));

        final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
            sink -> {
                sink.next(receivedMessage);
                sink.complete();
            }).subscribeOn(Schedulers.single());

        when(receiverClient.receiveMessages(1)).thenReturn(new IterableStream<>(iterableStreamFlux));
    }

}
