package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ResetProblemMessageJobTest {

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    private ResetProblemMessageJob resetProblemMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2");

    @BeforeEach
    void setUp() {
        resetProblemMessageJob = new ResetProblemMessageJob(caseEventMessageRepository, messageIds);
    }

    @Test
    void should_be_able_to_run_find_problem_message_job() {
        assertFalse(resetProblemMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
        assertTrue(resetProblemMessageJob.canRun(JobName.RESET_PROBLEM_MESSAGES));
    }

    @Test
    void should_return_empty_response_for_empty_message_ids() {
        assertTrue(new ResetProblemMessageJob(caseEventMessageRepository, null).run().isEmpty());
        assertTrue(new ResetProblemMessageJob(caseEventMessageRepository, List.of()).run().isEmpty());
    }

    @Test
    void should_return_empty_response_for_empty_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(resetProblemMessageJob.run().isEmpty());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(buildMessageEntity("1", MessageState.NEW),
                                buildMessageEntity("2", MessageState.READY)));
        assertTrue(resetProblemMessageJob.run().isEmpty());
    }

    @Test
    void should_reset_for_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(resetProblemMessageJob.run().isEmpty());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(buildMessageEntity("1", MessageState.UNPROCESSABLE),
                                buildMessageEntity("2", MessageState.UNPROCESSABLE),
                                buildMessageEntity("3", MessageState.NEW),
                                buildMessageEntity("4", MessageState.READY)));
        List<String> resetMessages = resetProblemMessageJob.run();
        assertEquals(2, resetMessages.size());
        assertIterableEquals(List.of("1", "2"), resetMessages);
        verify(caseEventMessageRepository, times(1))
            .updateMessageState(MessageState.NEW, List.of("1", "2"));
    }

    private CaseEventMessageEntity buildMessageEntity(String id, MessageState state) {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        entity.setMessageId(id);
        entity.setState(state);

        return entity;
    }
}
