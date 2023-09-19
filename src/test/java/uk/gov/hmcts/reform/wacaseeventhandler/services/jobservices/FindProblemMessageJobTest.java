package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;

import java.util.Collections;
import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class FindProblemMessageJobTest {
    private static final String MESSAGE_ID = "messageId";
    final int messageTimeLimit = 60;
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    private FindProblemMessageJob findProblemMessageJob;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private CaseEventMessageMapper caseEventMessageMapper = new CaseEventMessageMapper(objectMapper);

    @BeforeEach
    void setUp() {

        findProblemMessageJob = new FindProblemMessageJob(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          messageTimeLimit);
    }

    @Test
    void should_be_able_to_run_find_problem_message_job() {
        assertTrue(findProblemMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
        assertFalse(findProblemMessageJob.canRun(JobName.RESET_PROBLEM_MESSAGES));
    }

    @Test
    void should_return_empty_list_when_no_unprocessable_message_is_found() {
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit)).thenReturn(Collections.emptyList());
        List<String> messages = findProblemMessageJob.run();
        assertEquals(0, messages.size());
        assertEquals(Collections.emptyList(), messages);
    }

    @Test
    void should_return_unprocessable_when_case_id_is_invalid() {
        CaseEventMessageEntity mockCaseEventMessageEntity = createMockCaseEventMessageEntity(
            MessageState.UNPROCESSABLE);

        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit))
            .thenReturn(Collections.singletonList(mockCaseEventMessageEntity));

        List<String> unprocessableMessages = findProblemMessageJob.run();
        assertEquals(1, unprocessableMessages.size());
        assertEquals(MESSAGE_ID, unprocessableMessages.get(0));
    }

    @Test
    void should_handle_ccd_case_event_asb_message_when_return_old_ready_messages() {
        final CaseEventMessageEntity mockCaseEventMessageEntity = createMockCaseEventMessageEntity(MessageState.READY);
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit))
            .thenReturn(Collections.singletonList(mockCaseEventMessageEntity));
        List<String> readyMessages = findProblemMessageJob.run();
        assertEquals(1, readyMessages.size());
        assertEquals(MESSAGE_ID, readyMessages.get(0));
    }

    @ExtendWith(OutputCaptureExtension.class)
    @Test
    void should_include_case_type_id_in_log(CapturedOutput log) {
        final CaseEventMessageEntity mockCaseEventMessageEntity = createMockCaseEventMessageEntity(MessageState.READY);
        when(caseEventMessageRepository.findProblemMessages(messageTimeLimit))
            .thenReturn(Collections.singletonList(mockCaseEventMessageEntity));
        List<String> caseEventMessages = findProblemMessageJob.run();
        await().ignoreException(Exception.class)
            .pollInterval(100, MILLISECONDS)
            .atMost(5, SECONDS)
            .untilAsserted(() -> {
                assertThat(log.getOut().contains("\"caseTypeId\" : \"CaseType_123\""));
            });
    }

    private CaseEventMessageEntity createMockCaseEventMessageEntity(MessageState messageState) {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        caseEventMessageEntity.setState(messageState);
        caseEventMessageEntity.setMessageContent("{\"EventInstanceId\":\"EventInstanceId_123\", "
                                                 + "\"EventTimeStamp\":\"2023-05-10T08:25:51.713379525\","
                                                 + "\"CaseId\":\"CaseId_123\","
                                                 + "\"CaseTypeId\":\"CaseType_123\","
                                                 + "\"EventId\":\"EventId_123\","
                                                 + "\"PreviousStateId\":\"\","
                                                 + "\"NewstateId\":\"NewstateId_123\","
                                                 + "\"UserId\":\"UserId_123\"}");
        return caseEventMessageEntity;
    }

}
