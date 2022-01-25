package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;

@Slf4j
@Component
@SuppressWarnings("PMD.DoNotUseThreads")
public class DatabaseMessageConsumer implements Runnable {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final CaseEventMessageMapper caseEventMessageMapper;
    private final CcdEventProcessor ccdEventProcessor;
    private final LaunchDarklyFeatureFlagProvider flagProvider;
    protected static final Map<Integer, Integer> RETRY_COUNT_TO_DELAY_MAP = new ConcurrentHashMap<>();


    public DatabaseMessageConsumer(CaseEventMessageRepository caseEventMessageRepository,
                                   CaseEventMessageMapper caseEventMessageMapper,
                                   CcdEventProcessor ccdEventProcessor,
                                   LaunchDarklyFeatureFlagProvider flagProvider) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.caseEventMessageMapper = caseEventMessageMapper;
        this.ccdEventProcessor = ccdEventProcessor;
        this.flagProvider = flagProvider;
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
    @Transactional
    public void run() {
        if (flagProvider.getBooleanValue(DLQ_DB_INSERT)) {
            CaseEventMessage caseEventMessage = selectNextMessage();
            processMessage(caseEventMessage);
        } else {
            log.trace("Feature flag '{}' evaluated to false. Did not start message processor thread",
                    DLQ_DB_INSERT.getKey());
        }
    }

    private CaseEventMessage selectNextMessage() {
        log.trace("Selecting next message for processing from the database");

        final CaseEventMessageEntity nextAvailableMessageReadyToProcess =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        return caseEventMessageMapper
                .mapToCaseEventMessage(SerializationUtils.clone(nextAvailableMessageReadyToProcess));
    }

    private void processMessage(CaseEventMessage caseEventMessage) {
        if (caseEventMessage == null) {
            log.info("No message to process");
        } else {
            final String caseEventMessageId = caseEventMessage.getMessageId();
            log.info("Processing message with id {} from the database", caseEventMessageId);
            try {
                ccdEventProcessor.processMessage(caseEventMessage);
                caseEventMessageRepository.updateMessageState(MessageState.PROCESSED, List.of(caseEventMessageId));
            } catch (FeignException fe) {
                processException(fe, caseEventMessage);
            } catch (Exception ex) {
                processError(caseEventMessage);
            }
        }
    }

    private void processException(FeignException fce, CaseEventMessage caseEventMessage) {
        final HttpStatus httpStatus = HttpStatus.valueOf(fce.status());
        final boolean retryableError = RestExceptionCategory.isRetryableError(httpStatus);

        if (retryableError) {
            log.info("Retryable error occurred when processing message with caseId {}",
                    caseEventMessage.getMessageId());
            processRetryableError(caseEventMessage);
        } else {
            // TODO - alternative...Also think about transactions
            // CaseEventMessageEntity caseEventMessageEntity
            //      = caseEventMessageMapper.mapToCaseEventMessageEntity(caseEventMessage);
            // caseEventMessageEntity.setState(MessageState.UNPROCESSABLE);
            // caseEventMessageRepository.save(caseEventMessageEntity);
            processError(caseEventMessage);
        }
    }

    private void processRetryableError(CaseEventMessage caseEventMessage) {
        int retryCount = caseEventMessage.getRetryCount() + 1;
        Integer newHoldUntilIncrement = RETRY_COUNT_TO_DELAY_MAP.get(retryCount);

        if (newHoldUntilIncrement != null) {
            LocalDateTime newHoldUntil = LocalDateTime.now().plusSeconds(newHoldUntilIncrement);
            String messageId = caseEventMessage.getMessageId();
            log.info("Updating values, retry_count {} and hold_until {} on case event message {}",
                    retryCount,
                    newHoldUntil,
                    messageId);
            caseEventMessageRepository.updateMessageWithRetryDetails(retryCount,
                    newHoldUntil,
                    messageId);
        }
    }

    private void processError(CaseEventMessage caseEventMessage) {
        String caseEventMessageId = caseEventMessage.getMessageId();
        log.info("Could not process message with id {}, setting state to Unprocessable",
                caseEventMessage.getMessageId());
        caseEventMessageRepository.updateMessageState(MessageState.UNPROCESSABLE, List.of(caseEventMessageId));
    }
}
