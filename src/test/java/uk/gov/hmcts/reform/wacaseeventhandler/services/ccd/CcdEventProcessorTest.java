package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wacaseeventhandler.util.TestFixtures.createCaseEventMessage;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class CcdEventProcessorTest {

    @Mock
    private InitiationCaseEventHandler initiationTaskHandler;

    @Mock
    private ObjectMapper mapper;

    private CcdEventProcessor processor;

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() throws JsonProcessingException {

        EvaluateDmnResponse<InitiateEvaluateResponse> dmnResponse =
            new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(dmnResponse.getResults()).when(initiationTaskHandler).evaluateDmn(any(EventInformation.class));

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper);

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

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper);

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

        processor = new CcdEventProcessor(handlerServices, mapper);

        String incomingMessage = asJsonString(buildEventInformation());
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildEventInformation());

        processor.processMessage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);
        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }

    @Test
    void test_EventInformation_logging(CapturedOutput output) throws JsonProcessingException {

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdEventProcessor(handlerServices, mapper);

        String incomingMessage = asJsonString(buildEventInformation(true, true));
        when(mapper.readValue(incomingMessage, EventInformation.class))
            .thenReturn(buildEventInformation(true, true));

        processor.processMessage(incomingMessage);

        verify(mapper, Mockito.times(1))
            .readValue(incomingMessage, EventInformation.class);

        output.length();

        await().ignoreException(Exception.class)
            .pollInterval(100, MILLISECONDS)
            .atMost(5, SECONDS)
            .untilAsserted(() -> {
                assertThat(output).contains("Case details: \n");
                assertThat(output).contains("Additional data: \n");
            });
    }

    public String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    private EventInformation buildEventInformation() {
        return buildEventInformation(false, false);
    }

    private EventInformation buildEventInformation(boolean addAdditionalData, boolean withData) {

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
            .additionalData(getAdditionalData(addAdditionalData, withData))
            .build();
    }

    private AdditionalData getAdditionalData(boolean addAdditionalData, boolean withData) {

        if (!addAdditionalData) {
            return null;
        }
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of(
                "dateDue", "2021-04-06",
                "uniqueId", "",
                "directionType", ""
            ),
            "appealType", "protection"
        );

        return AdditionalData.builder()
            .data(withData ? dataMap : null)
            .build();
    }

}
