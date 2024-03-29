package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationMetadata;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformationRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageDuplicateMessageIdException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.TooManyMethods")
public class EventMessageReceiverService {
    protected static final String MESSAGE_PROPERTIES = "MessageProperties";

    private final ObjectMapper objectMapper;
    private final CaseEventMessageRepository repository;
    private final CaseEventMessageMapper mapper;

    public EventMessageReceiverService(ObjectMapper objectMapper,
                                       CaseEventMessageRepository repository,
                                       CaseEventMessageMapper caseEventMessageMapper) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.mapper = caseEventMessageMapper;
    }

    public CaseEventMessage handleDlqMessage(String messageId, String sessionId, String message) {
        log.info("Received Case Event Dead Letter Queue message with id '{}'", messageId);
        return handleMessage(messageId, sessionId,  message, true);
    }

    public CaseEventMessage handleAsbMessage(String messageId, String sessionId, String message) {
        log.info("Received ASB message with id '{}'", messageId);
        return handleMessage(messageId, sessionId, message, false);
    }

    public CaseEventMessage handleCcdCaseEventAsbMessage(String messageId, String sessionId, String message) {
        log.debug("Received CCD Case Events ASB message with id '{}'", messageId);

        return handleMessage(messageId, sessionId, message, false);
    }

    public CaseEventMessage getMessage(String messageId) {
        CaseEventMessageEntity messageEntity = repository.findByMessageId(singletonList(messageId)).stream().findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));
        return mapper.mapToCaseEventMessage(messageEntity);
    }

    private CaseEventMessageEntity insertMessage(CaseEventMessageEntity caseEventMessageEntity) {

        final List<CaseEventMessageEntity> existingMessages =
                repository.findByMessageId(singletonList(caseEventMessageEntity.getMessageId()));
        if (existingMessages.isEmpty()) {
            return repository.save(caseEventMessageEntity);
        } else {
            CaseEventMessageEntity found = existingMessages.get(0);

            found.setDeliveryCount(found.getDeliveryCount() + 1);
            return repository.save(found);
        }
    }

    public void deleteMessage(String messageId) {
        CaseEventMessageEntity entity = repository.findByMessageId(singletonList(messageId)).stream().findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));

        repository.deleteById(entity.getSequence());
    }

    public CaseEventMessage upsertMessage(String messageId, String sessionId, String message, Boolean fromDlq) {

        List<CaseEventMessageEntity> byMessageId = repository.findByMessageId(singletonList(messageId));
        if (byMessageId != null) {
            Optional<CaseEventMessageEntity> messageEntityOptional = byMessageId.stream().findFirst();
            if (messageEntityOptional.isPresent()) {
                try {
                    CaseEventMessageEntity messageEntity = buildCaseEventMessageEntity(messageId, message, fromDlq);
                    messageEntityOptional.ifPresent(eventMessageEntity -> messageEntity
                        .setSequence(eventMessageEntity.getSequence()));
                    repository.save(messageEntity);

                    log.info("Message with id '{}' successfully updated and saved into DB", messageId);

                    return mapper.mapToCaseEventMessage(messageEntity);
                } catch (JsonProcessingException e) {
                    log.error("Could not parse the message with id '{}'", messageId);

                    boolean isDlq = TRUE.equals(fromDlq);
                    CaseEventMessageEntity messageEntity = build(messageId, sessionId, message, isDlq,
                                                                 MessageState.UNPROCESSABLE);
                    repository.save(messageEntity);

                    return mapper.mapToCaseEventMessage(messageEntity);
                }
            }
        }

        return handleMessage(messageId, sessionId, message, fromDlq);
    }

    private CaseEventMessage handleMessage(String messageId, String sessionId, String message, Boolean fromDlq) {

        try {
            CaseEventMessageEntity messageEntity = buildCaseEventMessageEntity(messageId, message, fromDlq);
            CaseEventMessageEntity savedEntity = insertMessage(messageEntity);

            log.info("Message with id '{}' successfully stored into the DB", messageId);

            return mapper.mapToCaseEventMessage(savedEntity);
        } catch (JsonProcessingException e) {
            log.error("Could not parse the message with id '{}' case id '{}'", messageId, sessionId);

            boolean isDlq = TRUE.equals(fromDlq);
            CaseEventMessageEntity messageEntity = build(messageId, sessionId, message, isDlq,
                                                         MessageState.UNPROCESSABLE);
            CaseEventMessageEntity savedEntity = insertMessage(messageEntity);


            return mapper.mapToCaseEventMessage(savedEntity);
        } catch (DataIntegrityViolationException e) {
            throw new CaseEventMessageDuplicateMessageIdException(
                format("Trying to save a message with a duplicate messageId: %s", messageId), e);
        }
    }

    private CaseEventMessageEntity buildCaseEventMessageEntity(String messageId,
                                                               String message,
                                                               Boolean fromDlq)
        throws JsonProcessingException {

        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
        boolean isValid = validate(messageId, eventInformation, fromDlq);

        CaseEventMessageEntity messageEntity;
        if (isValid) {
            log.info("Message validation successful for message id {}", messageId);
            messageEntity = build(messageId, message, fromDlq, eventInformation, MessageState.NEW);
        } else {
            log.info("Message validation failed for message id {}", messageId);
            messageEntity = build(messageId, message, fromDlq, eventInformation, MessageState.UNPROCESSABLE);
        }

        EventInformationRequest eventInformationRequest = objectMapper.readValue(
            message,
            EventInformationRequest.class
        );
        EventInformationMetadata eventInformationMetadata = eventInformationRequest.getEventInformationMetadata();
        updateMessageEntity(messageEntity, eventInformationMetadata);

        return messageEntity;
    }

    private void updateMessageEntity(CaseEventMessageEntity messageEntity,
                                     EventInformationMetadata eventInformationMetadata) throws JsonProcessingException {
        JsonNode actualObj = convertMapToJsonNode(eventInformationMetadata);
        messageEntity.setMessageProperties(actualObj);
        messageEntity.setHoldUntil(eventInformationMetadata.getHoldUntil());
    }

    private JsonNode convertMapToJsonNode(EventInformationMetadata eventInformationMetadata)
        throws JsonProcessingException {

        String json = objectMapper.writeValueAsString(eventInformationMetadata.getMessageProperties());
        return objectMapper.readTree(json);
    }

    private CaseEventMessageEntity build(String messageId,
                                         String message,
                                         Boolean fromDlq,
                                         EventInformation eventInformation,
                                         MessageState state) {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(messageId);
        caseEventMessageEntity.setCaseId(eventInformation.getCaseId());
        caseEventMessageEntity.setEventTimestamp(eventInformation.getEventTimeStamp());
        caseEventMessageEntity.setFromDlq(fromDlq);
        caseEventMessageEntity.setState(state);
        caseEventMessageEntity.setMessageContent(message);
        caseEventMessageEntity.setReceived(LocalDateTime.now());
        caseEventMessageEntity.setDeliveryCount(0);
        caseEventMessageEntity.setRetryCount(0);

        return caseEventMessageEntity;
    }

    private CaseEventMessageEntity build(String messageId,
                                         String sessionId,
                                         String message,
                                         boolean fromDlq,
                                         MessageState state) {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(messageId);
        caseEventMessageEntity.setCaseId(sessionId);
        caseEventMessageEntity.setFromDlq(fromDlq);
        caseEventMessageEntity.setState(state);
        caseEventMessageEntity.setMessageContent(message);
        caseEventMessageEntity.setReceived(LocalDateTime.now());
        caseEventMessageEntity.setDeliveryCount(0);
        caseEventMessageEntity.setRetryCount(0);

        return caseEventMessageEntity;
    }

    private boolean validate(String messageId, EventInformation eventInformation, Boolean fromDlq) {

        log.info("Message validation for message id {} - [case id : {}, event timestamp : {}, from DLQ {}]",
                 messageId, eventInformation.getCaseId(), eventInformation.getEventTimeStamp(), fromDlq);
        return isNotBlank(eventInformation.getCaseId())
            && isNotBlank(messageId)
            && eventInformation.getEventTimeStamp() != null
            && isNotBlank(eventInformation.getEventTimeStamp().toString())
            && fromDlq != null;
    }


}
