package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName.SET_STATE_TO_PROCESSED_ON_MESSAGES;

@Service
@Slf4j
@Transactional
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class SetStateToProcessedMessageJob implements MessageJob {
    private final CaseEventMessageRepository caseEventMessageRepository;
    private final List<String> messageIds;

    public SetStateToProcessedMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                                         @Value("${job.problem-message.set-processed-state-message-id-list}")
                                         List<String> messageIds) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.messageIds = messageIds;
    }

    @Override
    public boolean canRun(JobName jobName) {
        return this.messageIds != null
                && !this.messageIds.isEmpty()
                && SET_STATE_TO_PROCESSED_ON_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {
        log.info("Start {}:'{}'", SET_STATE_TO_PROCESSED_ON_MESSAGES.name(), this.messageIds);

        if (!canRun(SET_STATE_TO_PROCESSED_ON_MESSAGES)) {
            return List.of();
        }

        log.info("Setting message state to {}", MessageState.PROCESSED);
        List<CaseEventMessageEntity> messages = caseEventMessageRepository.findByMessageId(this.messageIds);

        List<CaseEventMessageEntity> setMessageStateList = messages.stream()
            .filter(msg -> MessageState.UNPROCESSABLE.equals(msg.getState()))
            .collect(Collectors.toList());

        if (setMessageStateList.isEmpty()) {
            log.info(
                "{} There is no any UNPROCESSABLE message with setting message state",
                SET_STATE_TO_PROCESSED_ON_MESSAGES.name()
            );
            return List.of();
        }

        setMessageStateList.stream().forEach(messageEntity -> {
            log.info(
                "{} message id:{}, case id:{}, message state:{} and set to {}",
                SET_STATE_TO_PROCESSED_ON_MESSAGES.name(),
                messageEntity.getMessageId(),
                messageEntity.getCaseId(),
                messageEntity.getState(),
                MessageState.PROCESSED
            );
            messageEntity.setState(MessageState.PROCESSED);
        });

        return this.messageIds;
    }
}
