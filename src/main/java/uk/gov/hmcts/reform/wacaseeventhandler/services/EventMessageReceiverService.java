package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import static uk.gov.hmcts.reform.wacaseeventhandler.config.features.FeatureFlag.DLQ_DB_INSERT;


@Slf4j
@Service
@Transactional
public class EventMessageReceiverService {
    private final ObjectMapper objectMapper;
    private final LaunchDarklyFeatureFlagProvider featureFlagProvider;

    public EventMessageReceiverService(ObjectMapper objectMapper, LaunchDarklyFeatureFlagProvider featureFlagProvider) {
        this.objectMapper = objectMapper;
        this.featureFlagProvider = featureFlagProvider;
    }

    public void handleDlqMessage(String messageId, String message) {
        log.info("Received Case Event Dead Letter Queue message with id '{}'", messageId);
        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
            handleMessage(messageId, eventInformation, true, "Case Event Dead Letter Queue");
        } catch (JsonProcessingException e) {
            log.error("Could not parse the message with id '{}'", messageId);
        }
    }

    public void handleAsbMessage(String messageId, String message) {
        log.info("Received ASB message with id '{}'", messageId);
        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
            handleMessage(messageId, eventInformation, true, "Case Event");
        } catch (JsonProcessingException e) {
            log.error("Could not parse the message with id '{}'", messageId);
        }
    }

    public void handleCcdCaseEventAsbMessage(String messageId, String message) {
        try {
            log.info("Received CCD Case Events ASB message with id '{}'", messageId);
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);

            if (featureFlagProvider.getBooleanValue(DLQ_DB_INSERT, eventInformation.getUserId())) {
                handleMessage(messageId, eventInformation, true, "Case Event Dead Letter Queue");
            } else {
                log.info("Feature flag '{}' evaluated to false. Message not inserted into DB", DLQ_DB_INSERT.getKey());
            }

        } catch (JsonProcessingException e) {
            log.error("Could not parse the message with id '{}'", messageId);
        }
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void handleMessage(String messageId, EventInformation eventInformation, boolean fromDlq, String eventName) {
        log.info(
                "{} details:\n"
                        + "Case id: '{}'\n"
                        + "Event id: '{}'\n"
                        + "New state id: '{}'\n"
                        + "Previous state id: '{}'\n"
                        + "Jurisdiction id: '{}'\n"
                        + "Case type id: '{}'",
                eventName,
                eventInformation.getCaseId(),
                eventInformation.getEventId(),
                eventInformation.getNewStateId(),
                eventInformation.getPreviousStateId(),
                eventInformation.getJurisdictionId(),
                eventInformation.getCaseTypeId()
        );

        log.info("Message with id '{}' successfully stored into the DB", messageId);
    }
}