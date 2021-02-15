package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.models.DeadLetterOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CcdEventException;

@Slf4j
@Service
public class DeadLetterService {

    private static final String MESSAGE_DESERIALIZATION_ERROR = "MessageDeserializationError";
    private static final String APPLICATION_PROCESSING_ERROR = "ApplicationProcessingError";

    private final ObjectMapper objectMapper;

    public DeadLetterService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DeadLetterOptions handleApplicationError(String deadLetterMsg, String expMessage) {
        try {
            EventInformation eventInformation = objectMapper.readValue(deadLetterMsg, EventInformation.class);

            DeadLetterMessage message = new DeadLetterMessage(copyEvent(eventInformation), expMessage);
            final String deadLetterDescription = objectMapper.writeValueAsString(message);

            return createResponse(APPLICATION_PROCESSING_ERROR, deadLetterDescription);
        } catch (JsonProcessingException exp) {
            //should not come here. Have to catch exception from json
            throw new CcdEventException("Unable to deserialize receivedMessage", exp);
        }
    }

    public DeadLetterOptions handleParsingError(String deadLetterMsg, String expMessage) {
        try {
            DeadLetterMessage message = new DeadLetterMessage(deadLetterMsg, expMessage);
            final String deadLetterDescription = objectMapper.writeValueAsString(message);

            return createResponse(MESSAGE_DESERIALIZATION_ERROR, deadLetterDescription);
        } catch (JsonProcessingException exp) {
            //should not come here. Have to catch exception from json
            log.error("Unable to serialize DeadLetterMessage", exp);
            throw new CcdEventException("Unable to deserialize receivedMessage", exp);
        }
    }

    private DeadLetterOptions createResponse(String reason, String errorDescription) {
        DeadLetterOptions deadLetterOptions = new DeadLetterOptions();
        deadLetterOptions.setDeadLetterReason(reason);
        deadLetterOptions.setDeadLetterErrorDescription(errorDescription);

        return deadLetterOptions;
    }


    private String copyEvent(EventInformation event) {
        return EventInformation.builder()
            .eventInstanceId(event.getEventInstanceId())
            .eventTimeStamp(event.getEventTimeStamp())
            .jurisdictionId(event.getJurisdictionId())
            .caseTypeId(event.getCaseTypeId())
            .eventId(event.getEventId())
            .previousStateId(event.getPreviousStateId())
            .newStateId(event.getNewStateId())
            .build().toString();
    }
}
