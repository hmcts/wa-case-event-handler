package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageDuplicateMessageIdException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageNotFoundException;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;

import static java.lang.String.format;

@Slf4j
@Service
@Transactional
public class EventMessageReceiverService {
    public static final String MESSAGE_PROPERTIES = "MessageProperties";

    private final ObjectMapper objectMapper;
    private final CaseEventMessageRepository repository;
    private final CaseEventMessageMapper mapper;

    @Autowired
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

    public CaseEventMessageEntity getMessage(String messageId) {
        return repository.findByMessageId(messageId).stream().findFirst()
            .orElseThrow(() -> new CaseEventMessageNotFoundException(
                format("Could not find a message with message_id: %s", messageId)));
    }

    private CaseEventMessage handleMessage(String messageId, String message, boolean fromDlq) {

        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);

            MessageState state = validate(messageId, eventInformation);

            CaseEventMessageEntity messageEntity = build(messageId, message, fromDlq, eventInformation, state);
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

        return caseEventMessageEntity;
    }

    private JsonNode getMessageProperties(String message) throws JsonProcessingException {
        JsonNode messageAsJson = objectMapper.readTree(message);
        return messageAsJson.findPath(MESSAGE_PROPERTIES);
    }

    private MessageState validate(String messageId, EventInformation eventInformation) {
        // check all required fields
        log.info("Validating message with id '{}'", messageId);
        if (StringUtils.isNotBlank(eventInformation.getCaseId())) {
            return MessageState.NEW;
        } else {
            return MessageState.UNPROCESSABLE;
        }
    }
}
