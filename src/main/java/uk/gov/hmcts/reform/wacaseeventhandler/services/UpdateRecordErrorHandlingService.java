package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState.*;

@Slf4j
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class UpdateRecordErrorHandlingService {

    private final CaseEventMessageRepository caseEventMessageRepository;

    public UpdateRecordErrorHandlingService(CaseEventMessageRepository caseEventMessageRepository) {
        this.caseEventMessageRepository = caseEventMessageRepository;
    }

    public void handleUpdateError(MessageState state, String messageId, int retryCount, LocalDateTime holdUntil) {
        CaseEventMessageEntity messageEntity = caseEventMessageRepository.findByMessageIdToUpdate(messageId)
            .stream()
            .findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));

        if(state != null) {
            if(PROCESSED.equals(messageEntity.getState())) {
                log.info("Message with message id {} is already updated", messageId);
            } else {
                caseEventMessageRepository.updateMessageState(state, List.of(messageId));
            }
        } else {
            if(UNPROCESSABLE.equals(messageEntity.getState()) || PROCESSED.equals(messageEntity.getState())) {
                log.info("Message with message id {} is already updated", messageId);
            } else {
                caseEventMessageRepository.updateMessageWithRetryDetails(retryCount, holdUntil, messageId);
            }
        }
    }
}

