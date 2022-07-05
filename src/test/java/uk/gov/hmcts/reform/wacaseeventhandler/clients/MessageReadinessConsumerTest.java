package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;
import uk.gov.hmcts.reform.wacaseeventhandler.util.TestFixtures;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReadinessConsumerTest {
    @Mock
    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @InjectMocks
    private MessageReadinessConsumer messageReadinessConsumer;

    @Test
    void should_not_modify_message_state_if_no_case_event_messages_returned_from_db() {
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(Collections.emptyList());

        messageReadinessConsumer.run();

        verify(caseEventMessageRepository, never()).updateMessageState(any(), any());
    }

    @Test
    void should_modify_message_state_feature_flag_enabled_messages_returned_from_db_and_dlq_empty() {
        final CaseEventMessageEntity caseEventMessageEntity = TestFixtures.createCaseEventMessageEntity();
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(List.of(caseEventMessageEntity));
        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(true);

        messageReadinessConsumer.run();

        verify(caseEventMessageRepository)
                .updateMessageState(MessageState.READY, List.of(caseEventMessageEntity.getMessageId()));
    }

    @Test
    void should_not_modify_message_state_feature_flag_enabled_messages_returned_from_db_and_dlq_not_empty() {
        final CaseEventMessageEntity caseEventMessageEntity = TestFixtures.createCaseEventMessageEntity();
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(List.of(caseEventMessageEntity));
        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(false);

        messageReadinessConsumer.run();

        verify(caseEventMessageRepository, never()).updateMessageState(any(), any());
    }
}
