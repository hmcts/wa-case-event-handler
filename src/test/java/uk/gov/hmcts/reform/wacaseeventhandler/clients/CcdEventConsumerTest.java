package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Ignore
@ExtendWith(MockitoExtension.class)
public class CcdEventConsumerTest {

    @Mock
    private InitiationTaskHandler initiationTaskHandler;

    @Mock
    private ObjectMapper objectMapper;

    private CcdEventConsumer client;

    @Test
    void should_consume_ccd_event() throws JsonProcessingException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        client = new CcdEventConsumer(handlerServices, objectMapper);

        String incomingMessage = asJsonString(buildMessage());

        Mockito.when(objectMapper.readValue(incomingMessage, EventInformation.class)).thenReturn(buildMessage());
        client.onMessage(incomingMessage, null, null);

        Mockito.verify(objectMapper, Mockito.times(1)).readValue(incomingMessage, EventInformation.class);
    }

    @Test
    void should_throw_parser_exception_when_consuming_ccd_event() throws JsonProcessingException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        client = new CcdEventConsumer(handlerServices, objectMapper);

        String incomingMessage = asJsonString(buildMessage());

        Mockito.when(objectMapper.readValue(incomingMessage, EventInformation.class))
            .thenThrow(JsonProcessingException.class);
        client.onMessage(incomingMessage, null, null);
    }

    public static String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    private EventInformation buildMessage() {
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(ZonedDateTime.now().plusDays(2).toLocalDateTime())
            .caseId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("requestRespondentEvidence")
            .newStateId("awaitingRespondentEvidence")
            .previousStateId("")
            .userId("some user Id")
            .build();

        return eventInformation;
    }
}
