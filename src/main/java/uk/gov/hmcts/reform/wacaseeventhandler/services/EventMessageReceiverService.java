package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageDuplicateMessageIdException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;

import static java.lang.String.format;
import static net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;


@Slf4j
@Service
@Transactional
public class EventMessageReceiverService {
    private static final String MESSAGE_PROPERTIES = "MessageProperties";
    private static final String USER_ID = "UserId";

    private final ObjectMapper objectMapper;
    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;
    private final CaseEventMessageRepository repository;
    private final CaseEventMessageMapper mapper;

    public EventMessageReceiverService(ObjectMapper objectMapper,
                                       CaseEventMessageRepository repository,
                                       CaseEventMessageMapper caseEventMessageMapper,
                                       LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.objectMapper = objectMapper;
        this.repository = repository;
        this.mapper = caseEventMessageMapper;
        this.featureFlagProvider = featureFlagProvider;
    }

    public CaseEventMessage handleDlqMessage(String messageId, String message) {
        log.info("Received Case Event Dead Letter Queue message with id '{}'", messageId);
        return handleMessage(messageId, message, true);
    }

    public CaseEventMessage handleAsbMessage(String messageId, String message) {
        log.info("Received ASB message with id '{}'", messageId);
        return handleMessage(messageId, message, true);
    }

    public CaseEventMessage handleCcdCaseEventAsbMessage(String messageId, String message) {
        log.info("Received CCD Case Events ASB message with id '{}'", messageId);

        if (featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, getUserId(message))) {
            return handleMessage(messageId, message, true);
        } else {
            log.info("Feature flag '{}' evaluated to false. Message not inserted into DB", DLQ_DB_INSERT.getKey());
        }

        return null;
    }

    public CaseEventMessage getMessage(String messageId) {
        CaseEventMessageEntity messageEntity = repository.findByMessageId(messageId).stream().findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message id: %s", messageId)));
        return mapper.mapToCaseEventMessage(messageEntity);
    }

    private CaseEventMessage handleMessage(String messageId,
                                           String message,
                                           boolean fromDlq) {

        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);

            boolean isValid = validate(messageId, eventInformation);
            CaseEventMessageEntity messageEntity;
            if (isValid) {
                messageEntity = build(messageId, message, fromDlq, eventInformation, MessageState.NEW);
            } else {
                messageEntity = build(messageId, message, fromDlq, MessageState.UNPROCESSABLE);
            }
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

    private CaseEventMessageEntity build(String messageId,
                                         String message,
                                         boolean fromDlq,
                                         EventInformation eventInformation,
                                         MessageState state) throws JsonProcessingException {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(messageId);
        caseEventMessageEntity.setCaseId(eventInformation.getCaseId());
        caseEventMessageEntity.setEventTimestamp(eventInformation.getEventTimeStamp());
        caseEventMessageEntity.setFromDlq(fromDlq);
        caseEventMessageEntity.setState(state);
        caseEventMessageEntity.setMessageProperties(getMessageProperties(message));
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

    private JsonNode getMessageProperties(String message) throws JsonProcessingException {
        JsonNode messageAsJson = objectMapper.readTree(message);
        return messageAsJson.findPath(MESSAGE_PROPERTIES);
    }

    public String getUserId(String message) {
        try {
            JsonNode messageAsJson = objectMapper.readTree(message);
            final JsonNode userIdNode = messageAsJson.findPath(USER_ID);
            if (!userIdNode.isMissingNode()) {
                String userIdTextValue = userIdNode.textValue();
                log.info("Returning User Id {} found in message", userIdTextValue);
                return userIdTextValue;
            }
        } catch (JsonProcessingException e) {
            log.info("Unable to find User Id in message");
        }
        return null;
    }

    private boolean validate(String messageId, EventInformation eventInformation) {
        // check all required fields
        log.info("Validating message with id '{}'", messageId);
        return isNotBlank(eventInformation.getCaseId()) && eventInformation.getEventTimeStamp() != null;
    }
}