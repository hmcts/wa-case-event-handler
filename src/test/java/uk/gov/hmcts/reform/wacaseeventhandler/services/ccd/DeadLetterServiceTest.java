package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
    public void setup() {
        deadLetterService = new DeadLetterService(mapper);
    }

    @Test
    void test_handle_parsing_error() throws JsonProcessingException {
        when(mapper.writeValueAsString(any())).thenReturn("DeadLetter Description");

        final DeadLetterOptions deadLetterOptions = deadLetterService.handleParsingError(
            "testMessage", "Parsing Error"
        );

        assertNotNull(deadLetterOptions);
        assertEquals("MessageDeserializationError", deadLetterOptions.getDeadLetterReason());
        assertEquals("DeadLetter Description", deadLetterOptions.getDeadLetterErrorDescription());
    }

    @Test
    void test_handle_parsing_error_with_json_exception() throws JsonProcessingException {
        when(mapper.writeValueAsString(any())).thenThrow(JsonParseException.class);

        final DeadLetterOptions deadLetterOptions = deadLetterService.handleParsingError(
            "testMessage", "Parsing Error"
        );

        assertNotNull(deadLetterOptions);
        assertEquals("MessageDeserializationError", deadLetterOptions.getDeadLetterReason());
        assertEquals("Unable to deserialize receivedMessage",
                                deadLetterOptions.getDeadLetterErrorDescription());
    }

    @Test
    void test_handle_application_error() throws JsonProcessingException {
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
    void test_handle_application_error_with_json_exception() throws JsonProcessingException {
        String event = createEvent();

        when(mapper.readValue(event, EventInformation.class)).thenReturn(eventInformation);
        when(mapper.writeValueAsString(any())).thenThrow(JsonParseException.class);

        final DeadLetterOptions deadLetterOptions = deadLetterService
            .handleApplicationError(event, "Downstream Error");

        assertNotNull(deadLetterOptions);
        assertEquals("ApplicationProcessingError", deadLetterOptions.getDeadLetterReason());
        assertEquals("Unable to deserialize receivedMessage",
                                deadLetterOptions.getDeadLetterErrorDescription());
    }

    private String createEvent() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper.writeValueAsString(eventInformation);
    }

}
