package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNoAllowedRequestException;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerTestingControllerTest {
    public static final String JSON_MESSAGE = "{\"jsonMessage\":\"anything\"}";
    public static final String MESSAGE_ID = "123";

    @Mock
    private EventMessageReceiverService eventMessageReceiverService;

    @Mock
    CaseEventMessage responseMessage;

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
}
