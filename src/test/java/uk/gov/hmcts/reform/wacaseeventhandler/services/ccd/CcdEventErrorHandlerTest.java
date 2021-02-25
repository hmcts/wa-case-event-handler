package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.core.amqp.models.AmqpAnnotatedMessage;
import com.azure.core.amqp.models.AmqpMessageHeader;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcdEventErrorHandlerTest {

    @Mock
    private DeadLetterService deadLetterService;
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
    private JsonParseException jsonParseException;
    @Mock
    private RestClientException restClientException;
    @Mock
    private Throwable throwable;

    private CcdEventErrorHandler underTest;

    @BeforeEach
    void setUp() {
        underTest = new CcdEventErrorHandler(deadLetterService, 2);
    }

    @Test
    void should_handle_json_error() {
        when(deadLetterService.handleParsingError(any(), any())).thenReturn(new DeadLetterOptions());
        doNothing().when(receiverClient).deadLetter(any(), any());


        underTest.handleJsonError(receiverClient, messageStream,
                                  "loggerMessage", "input", jsonParseException);

        verify(deadLetterService, Mockito.times(1))
            .handleParsingError(any(), any());
    }

    @Test
    void should_handle_application_error_with_message_abandon() {
        when(messageStream.getRawAmqpMessage()).thenReturn(amqpAnnotatedMessage);
        when(amqpAnnotatedMessage.getHeader()).thenReturn(header);
        when(header.getDeliveryCount()).thenReturn(1L);
        doNothing().when(receiverClient).abandon(any());

        underTest.handleApplicationError(receiverClient, messageStream,
                                     "loggerMessage", "input", restClientException);

        verify(receiverClient, Mockito.times(1))
            .abandon(any());
        verify(deadLetterService, Mockito.times(0))
            .handleApplicationError(any(), any());
    }

    @Test
    void should_handle_application_error_with_message_deadLettered() {
        //publishMessageToReceiver();

        when(messageStream.getRawAmqpMessage()).thenReturn(amqpAnnotatedMessage);
        when(amqpAnnotatedMessage.getHeader()).thenReturn(header);
        when(header.getDeliveryCount()).thenReturn(2L);

        when(deadLetterService.handleApplicationError(any(), any())).thenReturn(new DeadLetterOptions());
        doNothing().when(receiverClient).deadLetter(any(), any());

        underTest.handleApplicationError(receiverClient, messageStream,
                                         "loggerMessage", "input", restClientException);

        verify(receiverClient, Mockito.times(0))
            .abandon(any());
        verify(receiverClient, Mockito.times(1))
            .deadLetter(any(), any());
        verify(deadLetterService, Mockito.times(1))
            .handleApplicationError(any(), any());
    }

    @Test
    void should_handle_generic_error_with_unknown_error() {
        when(deadLetterService.handleApplicationError(any(), any())).thenReturn(new DeadLetterOptions());
        doNothing().when(receiverClient).deadLetter(any(), any());


        underTest.handleGenericError(receiverClient, messageStream,
                                  "loggerMessage", "input", throwable);

        verify(deadLetterService, Mockito.times(1))
            .handleApplicationError(any(), any());
    }

    @Test
    void should_handle_generic_error_with_known_error() {
        when(throwable.getMessage()).thenReturn("Null pointer exception");
        when(deadLetterService.handleApplicationError(any(), any())).thenReturn(new DeadLetterOptions());
        doNothing().when(receiverClient).deadLetter(any(), any());


        underTest.handleGenericError(receiverClient, messageStream,
                                     "loggerMessage", "input", throwable);

        verify(deadLetterService, Mockito.times(1))
            .handleApplicationError(any(), any());
    }

}
