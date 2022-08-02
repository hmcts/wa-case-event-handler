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

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName.RESET_PROBLEM_MESSAGES;

@Service
@Slf4j
@Transactional
public class ResetProblemMessageJob implements MessageJob {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final List<String> messageIds;

    public ResetProblemMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                                   @Value("${job.problem-message.message-id-list}")
                                   List<String> messageIds) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.messageIds = messageIds;
    }


    @Override
    public boolean canRun(JobName jobName) {
        return RESET_PROBLEM_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {
        log.info("Resetting problem messages for job name '{}' in message db", RESET_PROBLEM_MESSAGES.name());
        if (messageIds == null || messageIds.isEmpty()) {
            log.info("{} There is no any message id to reset", RESET_PROBLEM_MESSAGES.name());
            return List.of();
        }

        List<CaseEventMessageEntity> messages = caseEventMessageRepository.findByMessageId(messageIds);
        List<String> messagesToReset = messages.stream()
            .filter(msg -> MessageState.UNPROCESSABLE.equals(msg.getState()))
            .map(CaseEventMessageEntity::getMessageId)
            .collect(Collectors.toList());

        if (messagesToReset.isEmpty()) {
            log.info("{} There is no any UNPROCESSABLE message to reset", RESET_PROBLEM_MESSAGES.name());
            return List.of();
        }

        int updateCount = caseEventMessageRepository.updateMessageState(MessageState.NEW, messagesToReset);
        log.info("{} Reset total of {} problem messages to READY from the list '{}'",
                 RESET_PROBLEM_MESSAGES.name(),
                 updateCount,
                 messagesToReset);

        return messagesToReset;
    }
}
