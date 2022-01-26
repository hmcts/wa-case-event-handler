package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.TASK_INITIATION_FEATURE;
import static uk.gov.hmcts.reform.wacaseeventhandler.util.TestFixtures.createCaseEventMessage;

@ExtendWith(MockitoExtension.class)
class CcdEventProcessorTest {

    @Mock
    private InitiationCaseEventHandler initiationTaskHandler;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    private CcdEventProcessor processor;

    @Test
    void should_not_trigger_handlers_when_feature_flag_is_false() throws JsonProcessingException {

        when(featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE, "some user id"))
            .thenReturn(false);

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper, featureFlagProvider);

        String incomingMessage = asJsonString(buildEventInformation());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildEventInformation());

        processor.processMessage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);

        verify(initiationTaskHandler, never()).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler, never()).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() throws JsonProcessingException {

        EvaluateDmnResponse<InitiateEvaluateResponse> dmnResponse =
            new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(dmnResponse.getResults()).when(initiationTaskHandler).evaluateDmn(any(EventInformation.class));

        when(featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE, "some user id")).thenReturn(true);

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper, featureFlagProvider);

        String incomingMessage = asJsonString(buildEventInformation());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildEventInformation());

        processor.processMessage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle_case_event_message()
            throws JsonProcessingException {

        EvaluateDmnResponse<InitiateEvaluateResponse> dmnResponse =
                new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(dmnResponse.getResults()).when(initiationTaskHandler).evaluateDmn(any(EventInformation.class));

        when(featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE, "some user id")).thenReturn(true);

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper, featureFlagProvider);

        EventInformation eventInformation = buildEventInformation();
        String incomingMessage = asJsonString(eventInformation);
        CaseEventMessage caseEventMessage = createCaseEventMessage(eventInformation);

        when(mapper.readValue(incomingMessage, EventInformation.class))
                .thenReturn(eventInformation);

        processor.processMessage(caseEventMessage);

        verify(mapper, Mockito.times(1))
                .readValue(incomingMessage, EventInformation.class);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_nothing_then_caseEventHandler_does_not_handle() throws JsonProcessingException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        when(featureFlagProvider.getBooleanValue(TASK_INITIATION_FEATURE, "some user id")).thenReturn(true);
        processor = new CcdEventProcessor(handlerServices, mapper, featureFlagProvider);

        String incomingMessage = asJsonString(buildEventInformation());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildEventInformation());

        processor.processMessage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);
        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }

    public String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    private EventInformation buildEventInformation() {

        return EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(ZonedDateTime.now().plusDays(2).toLocalDateTime())
            .caseId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("requestRespondentEvidence")
            .newStateId("awaitingRespondentEvidence")
            .previousStateId("")
            .userId("some user id")
            .build();
    }

}
