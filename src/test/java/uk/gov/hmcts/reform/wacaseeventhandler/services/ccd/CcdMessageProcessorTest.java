package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcdMessageProcessorTest {

    @Mock
    private InitiationTaskHandler initiationTaskHandler;

    @Mock
    private ObjectMapper mapper;

    private CcdMessageProcessor processor;

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() throws JsonProcessingException {
        List<InitiateEvaluateResponse> results = List.of(InitiateEvaluateResponse.builder().build());
        when(initiationTaskHandler.evaluateDmn(any(EventInformation.class))).thenReturn(results);

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdMessageProcessor(handlerServices, mapper);

        String incomingMessage = asJsonString(buildMessage());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildMessage());

        processor.processMesssage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_nothing_then_caseEventHandler_does_not_handle() throws JsonProcessingException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        processor = new CcdMessageProcessor(handlerServices, mapper);

        String incomingMessage = asJsonString(buildMessage());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildMessage());

        processor.processMesssage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);
        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }

    public String asJsonString(final Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
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
