package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class ResetNullEventTimestampMessageJobTest {
    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private static EventInformation eventInformation;

    @Mock
    private static CaseEventMessageEntity caseEventMessageEntity;

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonProcessingException jsonProcessingException;

    private ResetNullEventTimestampMessageJob resetNullEventTimestampProblemMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2", "messageId_3");

    private Map<String, Object> caseEventMessageEntityMap = new HashMap<String, Object>();

    private final Logger logger = (Logger) LoggerFactory.getLogger(ResetNullEventTimestampMessageJob.class);

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();

        caseEventMessageEntityMap.clear();

        logger.addAppender(listAppender);

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
    void should_be_equal_when_event_timestamp_is_set() throws JsonProcessingException {
        EventInformation eventMessageFromEntity = mock(EventInformation.class);
        List<CaseEventMessageEntity> nullEventTimestampEntityList = new ArrayList<>();

        caseEventMessageEntityMap.put("messageId", "messageId_5");
        caseEventMessageEntityMap.put("eventTimeStamp", "2022-12-08T15:16:41.805481");
        CaseEventMessageEntity nullEventTimestampEntity = caseEventMessageEntity
            .buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

        nullEventTimestampEntityList.add(nullEventTimestampEntity);
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(nullEventTimestampEntityList);

        for (int timestamps = 0; timestamps < nullEventTimestampEntityList.size(); timestamps++) {
            CaseEventMessageEntity messages = nullEventTimestampEntityList.get(timestamps);
            when(objectMapper.readValue(messages.getMessageContent(), EventInformation.class))
                .thenReturn(eventMessageFromEntity);
            messages.setEventTimestamp(eventMessageFromEntity.getEventTimeStamp());
            assertEquals(eventMessageFromEntity.getEventTimeStamp(), nullEventTimestampEntity.getEventTimestamp());
        }

        resetNullEventTimestampProblemMessageJob.run();
    }

    @Test
    void should_return_empty_response_for_empty_unprocessable_messages() {
        when(caseEventMessageRepository.findByMessageId(messageIds)).thenReturn(List.of());
        assertTrue(resetNullEventTimestampProblemMessageJob.run().isEmpty());

        caseEventMessageEntityMap.put("messageId", "messageId_1");
        CaseEventMessageEntity messageOne = caseEventMessageEntity
            .buildMessageEntity(caseEventMessageEntityMap, MessageState.NEW);

        caseEventMessageEntityMap.clear();

        caseEventMessageEntityMap.put("messageId", "messageId_2");
        CaseEventMessageEntity messageTwo = caseEventMessageEntity
            .buildMessageEntity(caseEventMessageEntityMap, MessageState.READY);

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(messageOne, messageTwo));

        assertTrue(resetNullEventTimestampProblemMessageJob.run().isEmpty());
    }

    @Test
    void should_return_message_id_list_response_for_handling_null_event_timestamp_messages()
        throws JsonProcessingException {
        EventInformation eventMessageFromEntity = eventInformation.getEventInformation();

        caseEventMessageEntityMap.put("messageId", "messageId_3");
        CaseEventMessageEntity nullEventTimestampEntity = caseEventMessageEntity
            .buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

        nullEventTimestampEntity.setMessageContent(eventMessageFromEntity.toString());

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(nullEventTimestampEntity));
        when(objectMapper.readValue(nullEventTimestampEntity.getMessageContent(), EventInformation.class))
            .thenReturn(eventMessageFromEntity);

        assertEquals(messageIds,resetNullEventTimestampProblemMessageJob.run());
    }

    @Test
    void should_return_json_processing_exception_when_message_content_is_incorrect() throws JsonProcessingException {
        caseEventMessageEntityMap.put("messageId", "messageId_3");
        CaseEventMessageEntity nullEventTimestampEntity = caseEventMessageEntity
            .buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

        nullEventTimestampEntity.setCaseId("caseId_3");
        nullEventTimestampEntity.setMessageContent("{\"CaseId\":\"caseId_3\",\"EventTimeStamp\":\"ABC\"}");

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(nullEventTimestampEntity));
        when(objectMapper.readValue(nullEventTimestampEntity.getMessageContent(), EventInformation.class))
            .thenThrow(jsonProcessingException);

        resetNullEventTimestampProblemMessageJob.run();

        assertLogMessageContains(
            String.format("Cannot parse the message with null eventTimeStamp, message id:%s and case id:%s",
                          nullEventTimestampEntity.getMessageId(),
                          nullEventTimestampEntity.getCaseId()
            ));
    }

    private void assertLogMessageContains(String expectedMessage) {
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream()
                       .map(ILoggingEvent::getFormattedMessage)
                       .collect(Collectors.toList())
                       .contains(expectedMessage));
    }
}
