package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class ResetNullEventTimestampMessageJobTest {
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ResetNullEventTimestampMessageJob resetNullEventTimestampProblemMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2", "messageId_3");

    @BeforeEach
    public void setUp() {
        resetNullEventTimestampProblemMessageJob = new ResetNullEventTimestampMessageJob(
            caseEventMessageRepository,
            messageIds,
            objectMapper
        );
    }

    @Test
    void should_be_able_to_run_reset_null_event_timestamp_problem_message_job() {
        assertFalse(resetNullEventTimestampProblemMessageJob.canRun(JobName.FIND_PROBLEM_MESSAGES));
        assertFalse(resetNullEventTimestampProblemMessageJob.canRun(JobName.RESET_PROBLEM_MESSAGES));
        assertTrue(resetNullEventTimestampProblemMessageJob.canRun(JobName.RESET_NULL_EVENT_TIMESTAMP_MESSAGES));
    }

    @Test
    void should_return_empty_response_for_empty_message_ids() {
        assertTrue(new ResetNullEventTimestampMessageJob(caseEventMessageRepository, null, objectMapper)
                       .run().isEmpty());
        assertTrue(new ResetNullEventTimestampMessageJob(caseEventMessageRepository, List.of(), objectMapper)
                       .run().isEmpty());
    }

    @Test
    void should_return_empty_response_for_empty_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(resetNullEventTimestampProblemMessageJob.run().isEmpty());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(buildMessageEntity("messageId_1", MessageState.NEW),
                                buildMessageEntity("messageId_2", MessageState.READY)));
        assertTrue(resetNullEventTimestampProblemMessageJob.run().isEmpty());
    }

    @Test
    void should_return_message_id_list_response_for_handling_null_event_timestampe_messages()
        throws JsonProcessingException {
        EventInformation eventMessageFromEntity = getEventInformation();

        CaseEventMessageEntity nullEventTimestampEntity = buildMessageEntity("messageId_3", MessageState.UNPROCESSABLE);
        nullEventTimestampEntity.setMessageContent(eventMessageFromEntity.toString());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(nullEventTimestampEntity));
        when(objectMapper.readValue(nullEventTimestampEntity.getMessageContent(), EventInformation.class))
            .thenReturn(eventMessageFromEntity);

        assertEquals(messageIds,resetNullEventTimestampProblemMessageJob.run());
    }

    private EventInformation getEventInformation() {
        return EventInformation
            .builder()
            .userId("userId_3")
            .jurisdictionId("jurisdictionId_3")
            .caseTypeId("caseTypeId_3")
            .caseId("caseId_3")
            .eventTimeStamp(LocalDateTime.now())
            .build();
    }

    private CaseEventMessageEntity buildMessageEntity(String id, MessageState state) {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();
        entity.setMessageId(id);
        entity.setState(state);

        return entity;
    }

}
