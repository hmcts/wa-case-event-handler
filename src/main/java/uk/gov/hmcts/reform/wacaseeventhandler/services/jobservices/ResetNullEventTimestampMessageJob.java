package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName.RESET_NULL_EVENT_TIMESTAMP_MESSAGES;

@Service
@Slf4j
@Transactional
public class ResetNullEventTimestampMessageJob implements MessageJob {
    private final CaseEventMessageRepository caseEventMessageRepository;
    private final List<String> messageIds;
    private final ObjectMapper objectMapper;

    public ResetNullEventTimestampMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                                             @Value("${job.problem-message.null-event-timestamp-message-id-list}")
                                             List<String> messageIds,
                                             ObjectMapper objectMapper) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.messageIds = messageIds;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canRun(JobName jobName) {
        return RESET_NULL_EVENT_TIMESTAMP_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {
        log.info("Start {}:'{}'", RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(), this.messageIds);

        if (this.messageIds == null || this.messageIds.isEmpty() || !canRun(RESET_NULL_EVENT_TIMESTAMP_MESSAGES)) {
            return List.of();
        }

        log.info("Resetting message with null eventTimestamp messages");
        List<CaseEventMessageEntity> messages = caseEventMessageRepository.findByMessageId(this.messageIds);

        List<CaseEventMessageEntity> nullEventTimestampMessageListToReset = messages.stream()
            .filter(msg -> MessageState.UNPROCESSABLE.equals(msg.getState()) && msg.getEventTimestamp() == null)
            .collect(Collectors.toList());

        if (nullEventTimestampMessageListToReset.isEmpty()) {
            log.info(
                "{} There is no any UNPROCESSABLE message with null eventTimestamp",
                RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name()
            );
            return List.of();
        }

        nullEventTimestampMessageListToReset.stream().forEach(messageEntity -> {
            try {
                EventInformation eventInformation = objectMapper.readValue(
                    messageEntity.getMessageContent(),
                    EventInformation.class
                );

                log.info(
                    "{} message id:{}, case id:{}, main eventTimeStamp:{}, messageContent eventTimeStamp:{}",
                    RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(),
                    messageEntity.getMessageId(),
                    messageEntity.getCaseId(),
                    messageEntity.getEventTimestamp(),
                    eventInformation.getEventTimeStamp()
                );

                messageEntity.setEventTimestamp(eventInformation.getEventTimeStamp());

                log.info(
                    "{} Completed reset main eventTimestamp to {} for message id:{} and case id:{}",
                    RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(),
                    messageEntity.getEventTimestamp(),
                    messageEntity.getMessageId(),
                    messageEntity.getCaseId()
                );
            } catch (JsonProcessingException jsonProcessingException) {
                log.info(
                    "Cannot parse the message with null eventTimeStamp, message id:{} and case id:{}",
                    messageEntity.getMessageId(),
                    messageEntity.getCaseId()
                );
            }
        });
        return this.messageIds;
    }
}

