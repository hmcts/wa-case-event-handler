package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.microsoft.applicationinsights.TelemetryClient;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.MessageUpdateRetry;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.UpdateRecordErrorHandlingService;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;
import uk.gov.hmcts.reform.wacaseeventhandler.util.UserIdParser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_PROCESS;

@Slf4j
@Component
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.DataflowAnomalyAnalysis"})
@Transactional(propagation = NOT_SUPPORTED)
@Profile("!functional & !local")
public class DatabaseMessageConsumer implements Runnable {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final CaseEventMessageMapper caseEventMessageMapper;
    private final CcdEventProcessor ccdEventProcessor;
    private final LaunchDarklyFeatureFlagProvider flagProvider;
    private final UpdateRecordErrorHandlingService updateRecordErrorHandlingService;
    private final TransactionTemplate transactionTemplate;
    protected static final Map<Integer, Integer> RETRY_COUNT_TO_DELAY_MAP = new ConcurrentHashMap<>();
    private final TelemetryClient telemetryClient;


    public DatabaseMessageConsumer(CaseEventMessageRepository caseEventMessageRepository,
                                   CaseEventMessageMapper caseEventMessageMapper,
                                   CcdEventProcessor ccdEventProcessor,
                                   LaunchDarklyFeatureFlagProvider flagProvider,
                                   UpdateRecordErrorHandlingService updateRecordErrorHandlingService,
                                   PlatformTransactionManager transactionManager,
                                   TelemetryClient telemetryClient) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.caseEventMessageMapper = caseEventMessageMapper;
        this.ccdEventProcessor = ccdEventProcessor;
        this.flagProvider = flagProvider;
        this.updateRecordErrorHandlingService = updateRecordErrorHandlingService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.telemetryClient = telemetryClient;
    }

    static {
        RETRY_COUNT_TO_DELAY_MAP.put(1, 5);
        RETRY_COUNT_TO_DELAY_MAP.put(2, 15);
        RETRY_COUNT_TO_DELAY_MAP.put(3, 30);
        RETRY_COUNT_TO_DELAY_MAP.put(4, 60);
        RETRY_COUNT_TO_DELAY_MAP.put(5, 300);
        RETRY_COUNT_TO_DELAY_MAP.put(6, 900);
        RETRY_COUNT_TO_DELAY_MAP.put(7, 1800);
        RETRY_COUNT_TO_DELAY_MAP.put(8, 3600);
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        Optional<MessageUpdateRetry> updateRetry = transactionTemplate.execute(status -> {

            CaseEventMessageEntity caseEventMessageEntity = selectNextMessage();

            if (caseEventMessageEntity == null) {
                log.trace("No message returned from database for processing");
            } else {
                log.info("Start message processing");

                if (flagProvider.getBooleanValue(DLQ_DB_PROCESS, getUserId(caseEventMessageEntity))) {
                    final CaseEventMessage caseEventMessage = caseEventMessageMapper
                        .mapToCaseEventMessage(SerializationUtils.clone(caseEventMessageEntity));

                    String operationId = UUID.randomUUID().toString().replaceAll("-", "");
                    log.info("Set operation id {} to process message {}", operationId, caseEventMessage.getMessageId());
                    telemetryClient.getContext().getOperation();//.setId(caseEventMessage.getMessageId());

                    Optional<MessageUpdateRetry> updatable = processMessage(caseEventMessage);

                    //if record state update failed, Rollback the transaction
                    updatable.ifPresent(r -> status.setRollbackOnly());
                    return updatable;
                } else {
                    log.trace(
                        "Feature flag '{}' evaluated to false. Did not start message processor thread",
                        DLQ_DB_PROCESS.getKey()
                    );
                }
            }
            return Optional.empty();
        });

        //Retry updating the record state
        updateRetry.ifPresent(msg ->
            updateRecordErrorHandlingService.handleUpdateError(msg.getState(),
                msg.getMessageId(),
                msg.getRetryCount(),
                msg.getHoldUntil())
        );

    }

    private String getUserId(CaseEventMessageEntity caseEventMessageEntity) {
        final String messageContent = caseEventMessageEntity.getMessageContent();
        if (messageContent != null) {
            return UserIdParser.getUserId(messageContent);
        }

        return null;
    }

    private CaseEventMessageEntity selectNextMessage() {
        log.trace("Selecting next message for processing from the database");

        return caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
    }

    private Optional<MessageUpdateRetry> processMessage(CaseEventMessage caseEventMessage) {
        if (caseEventMessage == null) {
            log.info("No message to process");
        } else {
            final String caseEventMessageId = caseEventMessage.getMessageId();
            log.info("Processing message with id: {} and caseId: {} from the database",
                caseEventMessageId,
                caseEventMessage.getCaseId()
            );
            try {
                ccdEventProcessor.processMessage(caseEventMessage);
                log.info("Message with id:{} and caseId:{} processed successfully, setting message state to PROCESSED",
                    caseEventMessageId,
                    caseEventMessage.getCaseId()
                );
                return updateMessageState(MessageState.PROCESSED, caseEventMessageId, 0, null);
            } catch (FeignException fe) {
                log.error("FeignException while processing message.", fe);
                return processException(fe, caseEventMessage);
            } catch (Exception ex) {
                log.error("Exception while processing message.", ex);
                return processError(caseEventMessage);
            }
        }
        return Optional.empty();
    }

    private Optional<MessageUpdateRetry> processException(FeignException fce, CaseEventMessage caseEventMessage) {
        boolean retryableError = false;
        try {
            final HttpStatus httpStatus = HttpStatus.valueOf(fce.status());
            retryableError = RestExceptionCategory.isRetryableError(httpStatus);
        } catch (IllegalArgumentException iae) {
            if (fce instanceof RetryableException) {
                retryableError = true;
            }
        }

        if (retryableError) {
            log.info("Retryable error occurred when processing message with messageId: {} and caseId: {}",
                caseEventMessage.getMessageId(),
                caseEventMessage.getCaseId()
            );
            return processRetryableError(caseEventMessage);
        } else {
            return processError(caseEventMessage);
        }
    }

    private Optional<MessageUpdateRetry> processRetryableError(CaseEventMessage caseEventMessage) {
        int retryCount = caseEventMessage.getRetryCount() + 1;
        Integer newHoldUntilIncrement = RETRY_COUNT_TO_DELAY_MAP.get(retryCount);
        String messageId = caseEventMessage.getMessageId();

        if (newHoldUntilIncrement != null) {
            LocalDateTime newHoldUntil = LocalDateTime.now().plusSeconds(newHoldUntilIncrement);
            log.info("Updating values, retry_count {} and hold_until {} on case event message {}",
                retryCount,
                newHoldUntil,
                messageId);
            return updateMessageState(null, messageId, retryCount, newHoldUntil);
        }
        return updateMessageState(MessageState.UNPROCESSABLE, messageId, 0, null);
    }

    private Optional<MessageUpdateRetry> processError(CaseEventMessage caseEventMessage) {
        String caseEventMessageId = caseEventMessage.getMessageId();
        log.info("Could not process message with id {} and caseId {}, setting state to Unprocessable",
            caseEventMessageId,
            caseEventMessage.getCaseId()
        );

        return updateMessageState(MessageState.UNPROCESSABLE, caseEventMessageId, 0, null);
    }

    private Optional<MessageUpdateRetry> updateMessageState(MessageState state, String messageId,
                                                            int retryCount, LocalDateTime holdUntil) {
        try {
            if (state == null) {
                caseEventMessageRepository.updateMessageWithRetryDetails(retryCount, holdUntil, messageId);
            } else {
                caseEventMessageRepository.updateMessageState(state, List.of(messageId));
            }
        } catch (RuntimeException e) {
            log.info("Error in updating message with id {}, retrying to update", messageId);
            return Optional.of(MessageUpdateRetry.builder()
                .messageId(messageId)
                .state(state)
                .holdUntil(holdUntil)
                .retryCount(retryCount)
                .build());
        }
        return Optional.empty();
    }
}
