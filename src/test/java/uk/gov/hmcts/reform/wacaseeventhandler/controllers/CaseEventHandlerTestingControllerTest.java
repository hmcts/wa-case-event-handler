package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNoAllowedRequestException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageQueryService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerTestingControllerTest {
    public static final String JSON_MESSAGE = "{\"jsonMessage\":\"anything\"}";
    public static final String MESSAGE_ID = "123";
    public static final String STATES = "NEW,UNPROCESSABLE";
    public static final String CASE_ID = "123";
    public static final String EVENT_TIMESTAMP = LocalDateTime.now().toString();
    public static final String FROM_DLQ = "true";

    @Mock
    private EventMessageReceiverService eventMessageReceiverService;

    @Mock
    private EventMessageQueryService eventMessageQueryService;

    @Mock
    CaseEventMessage responseMessage;

    @Mock
    EventMessageQueryResponse eventMessageQueryResponse;

    @InjectMocks
    CaseEventHandlerTestingController controller;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(controller, "environment", "local");
    }

    @Test
    void post_messages_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).handleAsbMessage(MESSAGE_ID, JSON_MESSAGE);

        CaseEventMessage response = controller.postCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            false
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).handleAsbMessage(MESSAGE_ID, JSON_MESSAGE);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void post_messages_should_delegate_to_eventMessageReceiverService_for_dlq_message() {
        doReturn(responseMessage).when(eventMessageReceiverService).handleDlqMessage(MESSAGE_ID, JSON_MESSAGE);

        CaseEventMessage response = controller.postCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            true
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).handleDlqMessage(MESSAGE_ID, JSON_MESSAGE);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void post_messages_should_throw_CaseEventMessageNoAllowedRequestException_when_in_prod_environment() {
        ReflectionTestUtils.setField(controller, "environment", "prod");

        assertThatThrownBy(() -> controller.postCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            true
        )).isInstanceOf(CaseEventMessageNoAllowedRequestException.class);
    }

    @Test
    void put_messages_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).upsertMessage(MESSAGE_ID, JSON_MESSAGE, false);

        CaseEventMessage response = controller.putCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            false
        );

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).upsertMessage(MESSAGE_ID, JSON_MESSAGE, false);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void put_messages_should_throw_CaseEventMessageNoAllowedRequestException_when_in_prod_environment() {
        ReflectionTestUtils.setField(controller, "environment", "prod");

        assertThatThrownBy(() -> controller.putCaseEventHandlerMessage(
            JSON_MESSAGE,
            MESSAGE_ID,
            true
        )).isInstanceOf(CaseEventMessageNoAllowedRequestException.class);
    }

    @Test
    void get_message_should_delegate_to_eventMessageReceiverService() {
        doReturn(responseMessage).when(eventMessageReceiverService).getMessage(MESSAGE_ID);

        CaseEventMessage response = controller.getMessagesByMessageId(MESSAGE_ID);

        assertThat(response).isEqualTo(responseMessage);

        verify(eventMessageReceiverService).getMessage(MESSAGE_ID);
        verifyNoMoreInteractions(eventMessageReceiverService);
    }

    @Test
    void get_message_should_throw_CaseEventMessageNoAllowedRequestException_when_in_prod_environment() {
        ReflectionTestUtils.setField(controller, "environment", "prod");

        assertThatThrownBy(() -> controller.getMessagesByMessageId(
            MESSAGE_ID
        )).isInstanceOf(CaseEventMessageNoAllowedRequestException.class);
    }

    @Test
    void should_delegate_to_eventMessageReceiverService_when_getMessagesByQueryParameters_called() {
        doReturn(eventMessageQueryResponse).when(eventMessageQueryService)
            .getMessages(STATES, CASE_ID, EVENT_TIMESTAMP, FROM_DLQ);

        EventMessageQueryResponse response = controller
            .getMessagesByQueryParameters(STATES, CASE_ID, EVENT_TIMESTAMP, FROM_DLQ);

        assertThat(response).isEqualTo(eventMessageQueryResponse);

        verify(eventMessageQueryService).getMessages(STATES, CASE_ID, EVENT_TIMESTAMP, FROM_DLQ);
    }

    @Test
    void should_throw_CaseEventMessageNoAllowedRequestException_when_getMessagesByQueryParameters_called_in_prod() {
        ReflectionTestUtils.setField(controller, "environment", "prod");

        assertThatThrownBy(() -> controller.getMessagesByQueryParameters(
            STATES, CASE_ID, EVENT_TIMESTAMP, FROM_DLQ
        )).isInstanceOf(CaseEventMessageNoAllowedRequestException.class);
    }
}
