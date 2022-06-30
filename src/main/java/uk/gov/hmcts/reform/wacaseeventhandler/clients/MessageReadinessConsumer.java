package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;
import uk.gov.hmcts.reform.wacaseeventhandler.util.UserIdParser;

import java.util.List;

@Slf4j
@Component
@SuppressWarnings("PMD.DoNotUseThreads")
@ConditionalOnProperty("azure.servicebus.enableASB-DLQ")
public class MessageReadinessConsumer implements Runnable {

    private final DeadLetterQueuePeekService deadLetterQueuePeekService;
    private final CaseEventMessageRepository caseEventMessageRepository;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    public MessageReadinessConsumer(DeadLetterQueuePeekService deadLetterQueuePeekService,
                                    CaseEventMessageRepository caseEventMessageRepository,
                                    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider) {
        this.deadLetterQueuePeekService = deadLetterQueuePeekService;
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
    }

    @Override
    @Transactional
    public void run() {
        final List<CaseEventMessageEntity> allMessageInNewState = caseEventMessageRepository.getAllMessagesInNewState();

        log.trace("Number of messages to check the readiness {}", allMessageInNewState.size());

        allMessageInNewState.stream()
                .filter(msg -> launchDarklyFeatureFlagProvider
                        .getBooleanValue(FeatureFlag.DLQ_DB_PROCESS, getUserId(msg)))
                .forEach(this::checkMessageToMoveToReadyState);
    }

    private void checkMessageToMoveToReadyState(CaseEventMessageEntity messageInNewState) {
        if (deadLetterQueuePeekService.isDeadLetterQueueEmpty()) {
            log.info("Updating following message to READY state {}", messageInNewState.getMessageId());
            caseEventMessageRepository.updateMessageState(MessageState.READY,
                    List.of(messageInNewState.getMessageId()));
        }
    }

    private String getUserId(CaseEventMessageEntity caseEventMessageEntity) {
        final String messageContent = caseEventMessageEntity.getMessageContent();
        if (messageContent != null) {
            return UserIdParser.getUserId(messageContent);
        }

        return null;
    }
}
