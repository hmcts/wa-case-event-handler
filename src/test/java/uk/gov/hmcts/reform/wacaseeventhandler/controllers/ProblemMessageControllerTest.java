package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.ProblemMessageService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @ParameterizedTest
    @CsvSource({
        "FIND_PROBLEM_MESSAGES",
        "RESET_PROBLEM_MESSAGES"
    })
    void should_process_job_request_and_return_response(JobName jobName) {
        when(problemMessageService.process(jobName)).thenReturn(List.of("message_id"));

        JobResponse response = controller.problemMessagesJob(jobName.name());

        assertNotNull(response);
        assertEquals(jobName.name(), response.getJobName());
        assertEquals(1, response.getNumberOfMessages());
        assertTrue(response.getMessageIds().contains("message_id"));
    }

    private CaseEventMessage createMockCaseEventMessage() {
        return CaseEventMessage.builder()
            .messageId("messageId")
            .build();
    }

}
