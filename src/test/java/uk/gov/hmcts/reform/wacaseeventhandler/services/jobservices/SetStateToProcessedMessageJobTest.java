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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})


class SetStateToProcessedMessageJobTest {
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    private SetStateToProcessedMessageJob setStateToProcessedMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2", "messageId_3");

    @BeforeEach
    void setUp() {
        setStateToProcessedMessageJob = new SetStateToProcessedMessageJob(
            caseEventMessageRepository,
            messageIds
        );
    }

    @Test
    void should_be_able_to_run_message_state_message_job() {
        assertFalse(setStateToProcessedMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
        assertFalse(setStateToProcessedMessageJob.canRun(JobName.RESET_PROBLEM_MESSAGES));
        assertFalse(setStateToProcessedMessageJob.canRun(JobName.RESET_NULL_EVENT_TIMESTAMP_MESSAGES));
        assertTrue(setStateToProcessedMessageJob.canRun(JobName.SET_STATE_TO_PROCESSED_ON_MESSAGES));
    }

    @Test
    void should_return_empty_response_for_empty_message_ids() {
        assertTrue(new SetStateToProcessedMessageJob(caseEventMessageRepository, null)
                       .run().isEmpty());
        assertTrue(new SetStateToProcessedMessageJob(caseEventMessageRepository, List.of())
                       .run().isEmpty());
    }

    @Test
    void should_return_empty_response_for_empty_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(setStateToProcessedMessageJob.run().isEmpty());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(new CaseEventMessageEntity().buildMessage("messageId_1", MessageState.NEW),
                                //new CaseEventMessageEntity().buildMessage("messageId_2", MessageState.READY),
                                new CaseEventMessageEntity().buildMessage("messageId_3", MessageState.PROCESSED)));
        assertTrue(setStateToProcessedMessageJob.run().isEmpty());
    }

    @Test
    void should_return_message_id_list_response_for_setting_message_state() {

        CaseEventMessageEntity unprocessableMessage = new CaseEventMessageEntity()
                                                          .buildMessage("messageId_3", MessageState.UNPROCESSABLE);

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(new CaseEventMessageEntity().buildMessage("messageId_1", MessageState.NEW),
                                new CaseEventMessageEntity().buildMessage("messageId_2", MessageState.READY),
                                unprocessableMessage));

        assertEquals(messageIds, setStateToProcessedMessageJob.run());
        assertEquals(MessageState.PROCESSED,unprocessableMessage.getState());
    }

}
