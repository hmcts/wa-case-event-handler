package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CcdEventMessageConsumerTest {

    @Mock
    private CcdMessageProcessor processor;

    private CcdEventMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new CcdEventMessageConsumer(processor);
    }

    @Test
    void given_valid_ccdMessage_when_seraialized_should_process_the_message() throws JsonProcessingException {
        String testMessage = "Test Message";

        doNothing().when(processor).processMesssage(testMessage);

        consumer.readMessage(testMessage.getBytes());

        verify(processor, Mockito.times(1)).processMesssage(testMessage);
    }

    @Test
    void given_invalid_ccdMessage_when_seraialized_should_not_throw_error() throws JsonProcessingException {
        String testMessage = "Test Message";

        doThrow(JsonProcessingException.class).when(processor).processMesssage(testMessage);

        consumer.readMessage(testMessage.getBytes());

        verify(processor, Mockito.times(1)).processMesssage(testMessage);
    }

}
