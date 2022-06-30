package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageErrorHandlingRepository;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState.PROCESSED;
import static uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState.UNPROCESSABLE;

@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class UpdateRecordErrorHandlingService {

    private final CaseEventMessageErrorHandlingRepository errorHandlingRepository;

    public UpdateRecordErrorHandlingService(CaseEventMessageErrorHandlingRepository errorHandlingRepository) {
        this.errorHandlingRepository = errorHandlingRepository;
    }

    public void handleUpdateError(MessageState state, String messageId, int retryCount, LocalDateTime holdUntil) {
        log.info("Retry updating message with message_id {}", messageId);
        CaseEventMessageEntity messageEntity = errorHandlingRepository.findByMessageIdToUpdate(messageId)
            .stream()
            .findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));

        if (state == null) {
            if (UNPROCESSABLE.equals(messageEntity.getState()) || PROCESSED.equals(messageEntity.getState())) {
                log.info("Message with message_id {} is already updated", messageId);
            } else {
                errorHandlingRepository.updateMessageWithRetryDetails(retryCount, holdUntil, messageId);
            }
        } else {
            if (PROCESSED.equals(messageEntity.getState())) {
                log.info("Message with message_id {} is already updated", messageId);
            } else {
                errorHandlingRepository.updateMessageState(state, List.of(messageId));
            }
        }
    }
}

