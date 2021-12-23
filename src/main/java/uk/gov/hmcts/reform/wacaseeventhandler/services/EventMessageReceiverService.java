package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import static java.lang.String.format;
import static net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Service
@Transactional
public class EventMessageReceiverService {
    public static final String MESSAGE_PROPERTIES = "MessageProperties";

    @Value("${environment}")
    private String environment;

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

    public CaseEventMessage handleDlqMessage(String messageId, String message) {
        log.info("Received DLQ message with id '{}'", messageId);
        return handleMessage(messageId, message, true);
    }

    public CaseEventMessage handleAsbMessage(String messageId, String message) {
        log.info("Received ASB message with id '{}'", messageId);
        return handleMessage(messageId, message, false);
    }

    public CaseEventMessage getMessage(String messageId) {
        CaseEventMessageEntity messageEntity = repository.findByMessageId(messageId).stream().findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));
        return mapper.mapToCaseEventMessage(messageEntity);
    }

    public CaseEventMessage upsertMessage(String messageId, String message, Boolean fromDlq) {

        List<CaseEventMessageEntity> byMessageId = repository.findByMessageId(messageId);
        if (byMessageId != null) {
            Optional<CaseEventMessageEntity> messageEntityOptional = byMessageId.stream().findFirst();
            if (messageEntityOptional.isPresent()) {
                try {

                    CaseEventMessageEntity messageEntity = buildCaseEventMessageEntity(messageId,
                                                                                       message, isDlq(fromDlq));
                    messageEntityOptional.ifPresent(eventMessageEntity -> messageEntity
                        .setSequence(eventMessageEntity.getSequence()));
                    repository.save(messageEntity);

                    log.info("Message with id '{}' successfully updated and saved into DB", messageId);

                    return mapper.mapToCaseEventMessage(messageEntity);
                } catch (JsonProcessingException e) {
                    log.error("Could not parse the message with id '{}'", messageId);

                    CaseEventMessageEntity messageEntity = build(messageId, message, isDlq(fromDlq),
                                                                 MessageState.UNPROCESSABLE);
                    repository.save(messageEntity);

                    return mapper.mapToCaseEventMessage(messageEntity);
                }
            }
        }

        return handleMessage(messageId, message, fromDlq);
    }

    public CaseEventMessage handleMessage(String messageId, String message, Boolean fromDlq) {

        try {
            CaseEventMessageEntity messageEntity = buildCaseEventMessageEntity(messageId, message, fromDlq);
            repository.save(messageEntity);

            log.info("Message with id '{}' successfully stored into the DB", messageId);

            return mapper.mapToCaseEventMessage(messageEntity);
        } catch (JsonProcessingException e) {
            log.error("Could not parse the message with id '{}'", messageId);

            CaseEventMessageEntity messageEntity = build(messageId, message, fromDlq, MessageState.UNPROCESSABLE);
            repository.save(messageEntity);

            return mapper.mapToCaseEventMessage(messageEntity);
        } catch (DataIntegrityViolationException e) {
            throw new CaseEventMessageDuplicateMessageIdException(
                format("Trying to save a message with a duplicate messageId: %s", messageId), e);
        }
    }

    private boolean isDlq(Boolean fromDlq) {
        return fromDlq != null && fromDlq;
    }

    private CaseEventMessageEntity buildCaseEventMessageEntity(String messageId,
                                                               String message,
                                                               boolean fromDlq)
        throws JsonProcessingException {

        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
        boolean isValid = validate(messageId, eventInformation);

        CaseEventMessageEntity messageEntity;
        if (isValid) {
            messageEntity = build(messageId, message, fromDlq, eventInformation, MessageState.NEW);
        } else {
            messageEntity = build(messageId, message, fromDlq, MessageState.UNPROCESSABLE);
        }

        log.info("Processing '{}' in '{}' environment ", messageId, environment);
        if (isNonProdEnvironment()) {
            EventInformationRequest eventInformationRequest = objectMapper.readValue(
                message,
                EventInformationRequest.class
            );
            EventInformationMetadata eventInformationMetadata = eventInformationRequest.getEventInformationMetadata();
            updateMessageEntity(messageEntity, eventInformationMetadata);
        }
        return messageEntity;
    }

    private void updateMessageEntity(CaseEventMessageEntity messageEntity,
                                     EventInformationMetadata eventInformationMetadata) {
        JsonNode actualObj = convertMapToJsonNode(eventInformationMetadata);
        messageEntity.setMessageProperties(actualObj);
        messageEntity.setHoldUntil(eventInformationMetadata.getHoldUntil());
    }

    private JsonNode convertMapToJsonNode(EventInformationMetadata eventInformationMetadata) {
        try {
            String json = objectMapper.writeValueAsString(eventInformationMetadata.getMessageProperties());
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Could not deserialize values");
            return objectMapper.createObjectNode();
        }
    }

    private boolean isNonProdEnvironment() {
        return !"prod".equalsIgnoreCase(environment);
    }

    private CaseEventMessageEntity build(String messageId,
                                         String message,
                                         boolean fromDlq,
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
                                         String message,
                                         boolean fromDlq,
                                         MessageState state) {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(messageId);
        caseEventMessageEntity.setFromDlq(fromDlq);
        caseEventMessageEntity.setState(state);
        caseEventMessageEntity.setMessageContent(message);
        caseEventMessageEntity.setReceived(LocalDateTime.now());
        caseEventMessageEntity.setDeliveryCount(0);
        caseEventMessageEntity.setRetryCount(0);

        return caseEventMessageEntity;
    }

    private boolean validate(String messageId, EventInformation eventInformation) {
        // check all required fields
        log.info("Validating message with id '{}'", messageId);
        return isNotBlank(eventInformation.getCaseId());
    }
}
