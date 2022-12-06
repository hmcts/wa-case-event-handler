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


public class SetMessageStateMessageJobTest {
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    private SetMessageStateMessageJob setMessageStateMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2", "messageId_3");

    @BeforeEach
    void setUp() {
        setMessageStateMessageJob = new SetMessageStateMessageJob(
            caseEventMessageRepository,
            messageIds
        );
    }

    @Test
    void should_be_able_to_run_message_state_message_job() {
        assertFalse(setMessageStateMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
        assertFalse(setMessageStateMessageJob.canRun(JobName.RESET_PROBLEM_MESSAGES));
        assertFalse(setMessageStateMessageJob.canRun(JobName.RESET_NULL_EVENT_TIMESTAMP_MESSAGES));
        assertTrue(setMessageStateMessageJob.canRun(JobName.SET_MESSAGE_STATE_MESSAGES));
    }

    @Test
    void should_return_empty_response_for_empty_message_ids() {
        assertTrue(new SetMessageStateMessageJob(caseEventMessageRepository, null)
                       .run().isEmpty());
        assertTrue(new SetMessageStateMessageJob(caseEventMessageRepository, List.of())
                       .run().isEmpty());
    }

    @Test
    void should_return_empty_response_for_empty_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(setMessageStateMessageJob.run().isEmpty());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(buildMessageEntity("messageId_1", MessageState.NEW),
                                buildMessageEntity("messageId_2", MessageState.READY),
                                buildMessageEntity("messageId_3", MessageState.PROCESSED)));
        assertTrue(setMessageStateMessageJob.run().isEmpty());
    }

    @Test
    void should_return_message_id_list_response_for_setting_message_state() {

        CaseEventMessageEntity unprocessableMessage = buildMessageEntity("messageId_3", MessageState.UNPROCESSABLE);

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(buildMessageEntity("messageId_1", MessageState.NEW),
                                buildMessageEntity("messageId_2", MessageState.READY),
                                unprocessableMessage));

        assertEquals(messageIds,setMessageStateMessageJob.run());
        assertEquals(MessageState.PROCESSED,unprocessableMessage.getState());
    }

    private CaseEventMessageEntity buildMessageEntity(String id, MessageState state) {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        entity.setMessageId(id);
        entity.setState(state);

        return entity;
    }
}
