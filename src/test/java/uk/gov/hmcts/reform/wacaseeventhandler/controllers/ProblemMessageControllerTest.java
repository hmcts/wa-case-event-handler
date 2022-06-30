package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ProblemMessageService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProblemMessageControllerTest {

    @Mock
    private ProblemMessageService problemMessageService;
    private ProblemMessageController controller;

    @BeforeEach
    void setup() {
        controller = new ProblemMessageController(problemMessageService);
    }

    @Test
    void should_send_unprocessable_job_request_and_return_empty_list() {
        List<CaseEventMessage> messages = Collections.emptyList();
        when(problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES))
            .thenReturn(messages);
        List<CaseEventMessage> response = controller.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES.name());
        assertEquals(0, response.size());
        assertEquals(Collections.emptyList(), response);
        verify(problemMessageService, times(1))
            .findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
    }

    @Test
    void should_send_unprocessable_job_request_and_return_a_valid_message_id_list() {
        List<CaseEventMessage> messageJobReport = Collections.singletonList(createMockCaseEventMessage());
        when(problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES))
            .thenReturn(messageJobReport);
        List<CaseEventMessage> response = controller.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES.name());
        assertEquals(1, response.size());
        assertEquals("messageId", response.get(0).getMessageId());
        verify(problemMessageService, times(1))
            .findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
    }



    @Test
    void should_send_ready_job_request_and_return_empty_list() {
        List<CaseEventMessage> messageJobReport = Collections.emptyList();
        when(problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES))
            .thenReturn(messageJobReport);
        List<CaseEventMessage> response = controller.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES.name());
        assertEquals(0, response.size());
        assertEquals(Collections.emptyList(), response);
        verify(problemMessageService, times(1))
            .findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
    }

    @Test
    void should_send_ready_job_request_and_return_a_valid_message_id_list() {
        List<CaseEventMessage> messages = Collections.singletonList(createMockCaseEventMessage());
        when(problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES)).thenReturn(messages);
        List<CaseEventMessage> response = controller.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES.name());
        assertEquals(1, response.size());
        assertEquals("messageId", response.get(0).getMessageId());
        verify(problemMessageService, times(1))
            .findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
    }

    private CaseEventMessage createMockCaseEventMessage() {
        return CaseEventMessage.builder()
            .messageId("messageId")
            .build();
    }

}
