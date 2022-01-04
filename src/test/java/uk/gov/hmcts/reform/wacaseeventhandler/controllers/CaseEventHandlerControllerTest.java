package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerControllerTest {
    public static final String JSON_MESSAGE = "{\"jsonMessage\":\"anything\"}";
    public static final String MESSAGE_ID = "123";

    @Mock
    private InitiationCaseEventHandler initiationTaskHandler;
    @Mock
    private EventMessageReceiverService eventMessageReceiverService;
    @Mock
    CaseEventMessage responseMessage;

    @Test
    void given_evaluateDmn_returns_nothing_then_caseEventHandler_does_not_handle() {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        CaseEventHandlerController controller = new CaseEventHandlerController(handlerServices,
                                                                               eventMessageReceiverService);

        ResponseEntity<Void> response = controller.caseEventHandler(
            EventInformation.builder()
                .jurisdictionId("ia")
                .caseTypeId("asylum")
                .build()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() {
        EvaluateDmnResponse<InitiateEvaluateResponse> dmnResponse =
            new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(dmnResponse.getResults()).when(initiationTaskHandler).evaluateDmn(any(EventInformation.class));

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        CaseEventHandlerController controller = new CaseEventHandlerController(handlerServices,
                                                                               eventMessageReceiverService);

        ResponseEntity<Void> response = controller.caseEventHandler(
            EventInformation.builder()
                .jurisdictionId("ia")
                .caseTypeId("asylum")
                .build()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void post_messages_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).handleAsbMessage(MESSAGE_ID, JSON_MESSAGE);

        CaseEventHandlerController controller = new CaseEventHandlerController(
            Collections.emptyList(),
            eventMessageReceiverService
        );

        CaseEventMessage response = controller.caseEventHandler(
            JSON_MESSAGE,
            MESSAGE_ID,
            false
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).handleAsbMessage(MESSAGE_ID, JSON_MESSAGE);
        verifyNoMoreInteractions(initiationTaskHandler);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void post_messages_should_delegate_to_eventMessageReceiverService_for_dlq_message() {
        doReturn(responseMessage).when(eventMessageReceiverService).handleDlqMessage(MESSAGE_ID, JSON_MESSAGE);

        CaseEventHandlerController controller = new CaseEventHandlerController(
            Collections.emptyList(),
            eventMessageReceiverService
        );

        CaseEventMessage response = controller.caseEventHandler(
            JSON_MESSAGE,
            MESSAGE_ID,
            true
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).handleDlqMessage(MESSAGE_ID, JSON_MESSAGE);
        verifyNoMoreInteractions(initiationTaskHandler);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void put_messages_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).upsertMessage(MESSAGE_ID, JSON_MESSAGE, false);

        CaseEventHandlerController controller = new CaseEventHandlerController(
            Collections.emptyList(),
            eventMessageReceiverService
        );

        CaseEventMessage response = controller.putCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            false
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).upsertMessage(MESSAGE_ID, JSON_MESSAGE, false);
        verifyNoMoreInteractions(initiationTaskHandler);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void get_message_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).getMessage(MESSAGE_ID);

        CaseEventHandlerController controller = new CaseEventHandlerController(
            Collections.emptyList(),
            eventMessageReceiverService
        );

        CaseEventMessage response = controller.getMessagesByMessageId(MESSAGE_ID);

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).getMessage(MESSAGE_ID);
        verifyNoMoreInteractions(initiationTaskHandler);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

}
