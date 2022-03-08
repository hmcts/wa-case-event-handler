package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class ProblemMessageServiceTest {
    private static final String MESSAGE_ID = "messageId";
    final int messageTimeLimit = 60;
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    private ProblemMessageService problemMessageService;

    @Spy
    private CaseEventMessageMapper caseEventMessageMapper = new CaseEventMessageMapper();

    @BeforeEach
    void setUp() {

        problemMessageService = new ProblemMessageService(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          messageTimeLimit);
    }

    @Test
    void should_return_empty_list_when_no_unprocessable_message_is_found() {
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit)).thenReturn(Collections.emptyList());
        List<CaseEventMessage> messages = problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
        assertEquals(0, messages.size());
        assertEquals(Collections.emptyList(), messages);
    }

    @Test
    void should_return_unprocessable_when_case_id_is_invalid() {
        CaseEventMessageEntity mockCaseEventMessageEntity = createMockCaseEventMessageEntity(
            MessageState.UNPROCESSABLE);

        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit))
            .thenReturn(Collections.singletonList(mockCaseEventMessageEntity));

        when(caseEventMessageMapper.mapToCaseEventMessage(mockCaseEventMessageEntity))
            .thenReturn(createMockCaseEventMessage(MessageState.UNPROCESSABLE));

        List<CaseEventMessage> unprocessableMessages = problemMessageService
            .findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
        assertEquals(1, unprocessableMessages.size());
        assertEquals(MESSAGE_ID, unprocessableMessages.get(0).getMessageId());
        assertEquals(MessageState.UNPROCESSABLE, unprocessableMessages.get(0).getState());
    }

    @Test
    void should_return_empty_list_when_no_ready_message_is_found() {
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit)).thenReturn(Collections.emptyList());
        List<CaseEventMessage> messages = problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
        assertEquals(0, messages.size());
        assertEquals(Collections.emptyList(), messages);
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_return_old_ready_messages() {
        final CaseEventMessageEntity mockCaseEventMessageEntity = createMockCaseEventMessageEntity(MessageState.READY);
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit))
            .thenReturn(Collections.singletonList(mockCaseEventMessageEntity));
        when(caseEventMessageMapper.mapToCaseEventMessage(mockCaseEventMessageEntity))
            .thenReturn(createMockCaseEventMessage(MessageState.READY));
        List<CaseEventMessage> readyMessages = problemMessageService.findProblemMessages(JobName.FIND_PROBLEM_MESSAGES);
        assertEquals(1, readyMessages.size());
        assertEquals(MESSAGE_ID, readyMessages.get(0).getMessageId());
        assertEquals(MessageState.READY, readyMessages.get(0).getState());

    }

    private CaseEventMessageEntity createMockCaseEventMessageEntity(MessageState messageState) {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        caseEventMessageEntity.setState(messageState);
        return caseEventMessageEntity;
    }

    private CaseEventMessage createMockCaseEventMessage(MessageState messageState) {
        return CaseEventMessage.builder()
            .messageId(MESSAGE_ID)
            .state(messageState)
            .build();

    }
}

