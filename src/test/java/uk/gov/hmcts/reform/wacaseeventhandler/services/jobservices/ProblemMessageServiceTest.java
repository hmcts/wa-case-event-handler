package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ProblemMessageServiceTest {
    @Mock
    private MessageJob messageJobFind;
    @Mock
    private MessageJob messageJobReset;

    ProblemMessageService problemMessageService;

    @BeforeEach
    void setUp() {
        problemMessageService = new ProblemMessageService(List.of(messageJobFind, messageJobReset));
    }

    @Test
    void should_run_message_job_if_only_pass_can_run_find() {
        doReturn(true).when(messageJobFind).canRun(JobName.FIND_PROBLEM_MESSAGES);
        problemMessageService.process(JobName.FIND_PROBLEM_MESSAGES);
        verify(messageJobFind, times(1)).run();
        verify(messageJobReset, times(1)).canRun(JobName.FIND_PROBLEM_MESSAGES);
    }

    @Test
    void should_run_message_job_if_only_pass_can_run_reset() {
        doReturn(true).when(messageJobReset).canRun(JobName.RESET_PROBLEM_MESSAGES);
        problemMessageService.process(JobName.RESET_PROBLEM_MESSAGES);
        verify(messageJobReset, times(1)).run();
        verify(messageJobFind, times(1)).canRun(JobName.RESET_PROBLEM_MESSAGES);
    }

    @Test
    void should_run_message_job_and_return_response() {
        doReturn(true).when(messageJobFind).canRun(JobName.FIND_PROBLEM_MESSAGES);
        when(messageJobFind.run()).thenReturn(List.of("some message"));
        List<String> response = problemMessageService.process(JobName.FIND_PROBLEM_MESSAGES);
        assertEquals("some message", response.get(0));
    }
}

