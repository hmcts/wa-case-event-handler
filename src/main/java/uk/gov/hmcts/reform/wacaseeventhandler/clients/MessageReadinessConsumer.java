package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.util.List;

@Slf4j
@Component
@SuppressWarnings("PMD.DoNotUseThreads")
@ConditionalOnProperty("azure.servicebus.enableASB-DLQ")
public class MessageReadinessConsumer implements Runnable {

    private final DeadLetterQueuePeekService deadLetterQueuePeekService;
    private final CaseEventMessageRepository caseEventMessageRepository;

    public MessageReadinessConsumer(DeadLetterQueuePeekService deadLetterQueuePeekService,
                                    CaseEventMessageRepository caseEventMessageRepository) {
        this.deadLetterQueuePeekService = deadLetterQueuePeekService;
        this.caseEventMessageRepository = caseEventMessageRepository;
    }

    @Override
    @Transactional
    public void run() {
        log.info("Running message readiness check");
        final List<CaseEventMessageEntity> allMessageInNewState = caseEventMessageRepository.getAllMessagesInNewState();

        log.info("Number of messages to check the readiness {}", allMessageInNewState.size());

        allMessageInNewState.stream()
                .forEach(this::checkMessageToMoveToReadyState);
    }

    private void checkMessageToMoveToReadyState(CaseEventMessageEntity messageInNewState) {
        if (deadLetterQueuePeekService.isDeadLetterQueueEmpty()) {
            log.info("Updating following message to READY state {}", messageInNewState.getMessageId());
            caseEventMessageRepository.updateMessageState(MessageState.READY,
                    List.of(messageInNewState.getMessageId()));
        }
    }
}
