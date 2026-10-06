package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.models.DeadLetterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeadLetterServiceTest {

    @Mock
    private ObjectMapper mapper;

    private DeadLetterService deadLetterService;

    private final EventInformation eventInformation = EventInformation.builder()
        .eventId("submitAppeal")
        .newStateId("")
        .jurisdictionId("ia")
        .caseTypeId("asylum")
        .caseId("caseId")
        .eventTimeStamp(LocalDateTime.now())
        .build();

    @BeforeEach
    void setup() {
        deadLetterService = new DeadLetterService(mapper);
    }

    @Test
    void test_handle_parsing_error() throws JacksonException {
        when(mapper.writeValueAsString(any())).thenReturn("DeadLetter Description");

        final DeadLetterOptions deadLetterOptions = deadLetterService.handleParsingError(
            "testMessage", "Parsing Error"
        );

        assertNotNull(deadLetterOptions);
        assertEquals("MessageDeserializationError", deadLetterOptions.getDeadLetterReason());
        assertEquals("DeadLetter Description", deadLetterOptions.getDeadLetterErrorDescription());
    }

    @Test
    void test_handle_parsing_error_with_json_exception() throws JacksonException {
        when(mapper.writeValueAsString(any())).thenThrow(StreamReadException.class);

        final DeadLetterOptions deadLetterOptions = deadLetterService.handleParsingError(
            "testMessage", "Parsing Error"
        );

        assertNotNull(deadLetterOptions);
        assertEquals("MessageDeserializationError", deadLetterOptions.getDeadLetterReason());
        assertEquals("Unable to deserialize receivedMessage",
                                deadLetterOptions.getDeadLetterErrorDescription());
    }

    @Test
    void test_handle_application_error() throws JacksonException {
        String event = createEvent();

        when(mapper.readValue(event, EventInformation.class)).thenReturn(eventInformation);
        when(mapper.writeValueAsString(any())).thenReturn(event);

        final DeadLetterOptions deadLetterOptions = deadLetterService
            .handleApplicationError(event, "Downstream Error");

        assertNotNull(deadLetterOptions);
        assertEquals("ApplicationProcessingError", deadLetterOptions.getDeadLetterReason());
        assertEquals(event, deadLetterOptions.getDeadLetterErrorDescription());
    }

    @Test
    void test_handle_application_error_with_json_exception() throws JacksonException {
        String event = createEvent();

        when(mapper.readValue(event, EventInformation.class)).thenReturn(eventInformation);
        when(mapper.writeValueAsString(any())).thenThrow(StreamReadException.class);

        final DeadLetterOptions deadLetterOptions = deadLetterService
            .handleApplicationError(event, "Downstream Error");

        assertNotNull(deadLetterOptions);
        assertEquals("ApplicationProcessingError", deadLetterOptions.getDeadLetterReason());
        assertEquals("Unable to deserialize receivedMessage",
                                deadLetterOptions.getDeadLetterErrorDescription());
    }

    private String createEvent() throws JacksonException {
        return JsonMapper.builder()
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
            .writeValueAsString(eventInformation);
    }

}
