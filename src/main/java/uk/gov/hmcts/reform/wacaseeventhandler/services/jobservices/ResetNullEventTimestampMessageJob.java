package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    public ResetNullEventTimestampMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                                  @Value("${job.problem-message.null-event-timestamp-message-id-list}")
                                      List<String> messageIds) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.messageIds = messageIds;
    }

    @Override
    public boolean canRun(JobName jobName) {
        return RESET_NULL_EVENT_TIMESTAMP_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {
        log.info("start {}:'{}'", RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(), this.messageIds);
        if(!this.messageIds.isEmpty()) {
            log.info("Resetting message with null eventTimestamp messages");
            ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
            List<CaseEventMessageEntity> messages = caseEventMessageRepository.findByMessageId(this.messageIds);

            List<CaseEventMessageEntity> nullEventTimestampMessageList = messages.stream()
                .filter(msg -> (MessageState.UNPROCESSABLE.equals(msg.getState()) && msg.getEventTimestamp() == null))
                .collect(Collectors.toList());

            if(nullEventTimestampMessageList.isEmpty()){
                log.info("{} There is no any UNPROCESSABLE message with null eventTimestamp", RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name());
                return List.of();
            }

            for (CaseEventMessageEntity obj : nullEventTimestampMessageList) {
                try {
                    EventInformation eventInformation = objectMapper.readValue(obj.getMessageContent(), EventInformation.class);
                    log.info("{} message id:{}, case id:{}, main eventTimeStamp:{}, messageContent eventTimeStamp:{}",
                             RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(),obj.getMessageId(), obj.getCaseId(), obj.getEventTimestamp(), eventInformation.getEventTimeStamp());

                    if (eventInformation.getEventTimeStamp() != null ) {
                        obj.setEventTimestamp(eventInformation.getEventTimeStamp());
                    }

                    log.info("{} Completed reset main eventTimestamp to {} for message id:{} and case id:{}",
                             RESET_NULL_EVENT_TIMESTAMP_MESSAGES.name(), obj.getEventTimestamp(), obj.getMessageId(), obj.getCaseId());
                } catch (JsonProcessingException jsonProcessingException) {
                    log.info("Cannot parse the message with null eventTimeStamp, message id:{} and case id:{}",
                             obj.getMessageId(),
                             obj.getCaseId());
                    jsonProcessingException.printStackTrace();
                }
            }
        }
        return this.messageIds;
    }
}
