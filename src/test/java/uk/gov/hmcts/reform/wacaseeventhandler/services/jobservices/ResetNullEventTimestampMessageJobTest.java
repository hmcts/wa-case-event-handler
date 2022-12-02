package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import javax.validation.constraints.AssertTrue;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
public class ResetNullEventTimestampMessageJobTest {
    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private JsonProcessingException jsonProcessingException;

    @Mock
    private Exception exception;

    private ResetNullEventTimestampMessageJob resetNullEventTimestampProblemMessageJob;

    private final List<String> messageIds = List.of("messageId_1", "messageId_2", "messageId_3");

    private Map<String, Object> caseEventMessageEntityMap = new HashMap<String, Object>();
    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(ResetNullEventTimestampMessageJob.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        caseEventMessageEntityMap.clear();

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
    void should_not_set_when_event_timestamp_is_null() throws JsonProcessingException {
        EventInformation eventMessageFromEntity = getEventInformation_one();

        caseEventMessageEntityMap.put("messageId", "messageId_4");
        CaseEventMessageEntity nullEventTimestampEntity = buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

        nullEventTimestampEntity.setMessageContent(eventMessageFromEntity.toString());

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

        caseEventMessageEntityMap.put("messageId", "messageId_1");
        CaseEventMessageEntity entity_one = buildMessageEntity(caseEventMessageEntityMap, MessageState.NEW);

        caseEventMessageEntityMap.put("messageId", "messageId_2");
        CaseEventMessageEntity entity_two = buildMessageEntity(caseEventMessageEntityMap, MessageState.READY);

        when(caseEventMessageRepository.findByMessageId(messageIds))
            .thenReturn(List.of(entity_one, entity_two));

        assertTrue(resetNullEventTimestampProblemMessageJob.run().isEmpty());
    }

    @Test
    void should_return_message_id_list_response_for_handling_null_event_timestampe_messages()
        throws JsonProcessingException {
        EventInformation eventMessageFromEntity = getEventInformation_one();

        caseEventMessageEntityMap.put("messageId", "messageId_3");
        CaseEventMessageEntity nullEventTimestampEntity = buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

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
        CaseEventMessageEntity nullEventTimestampEntity = buildMessageEntity(caseEventMessageEntityMap, MessageState.UNPROCESSABLE);

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

    private  static  EventInformation getEventInformation_one() {
        return EventInformation
            .builder()
            .userId("userId_3")
            .jurisdictionId("jurisdictionId_3")
            .caseTypeId("caseTypeId_3")
            .caseId("caseId_3")
            .eventTimeStamp(LocalDateTime.now())
            .build();
    }

    private CaseEventMessageEntity buildMessageEntity(Map<String, Object> map, MessageState state) {
        CaseEventMessageEntity entity = new CaseEventMessageEntity();

        for(Map.Entry<String, Object> val : map.entrySet())
        {
            Object value = val.getValue();
            switch(val.getKey())
            {
                case "messageId":
                    entity.setMessageId((String) value);
                    break;
                case "eventTimeStamp":

                    entity.setEventTimestamp(LocalDateTime.parse ((String) value));
                    break;
            }
        }

        entity.setState(state);

        return entity;
    }


    private void assertLogMessageContains(String expectedMessage) {
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.stream()
                       .map(ILoggingEvent::getFormattedMessage)
                       .collect(Collectors.toList())
                       .contains(expectedMessage));
    }
}
