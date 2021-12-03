package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

@Slf4j
@Service
public class MessageReceiver {

    private final ObjectMapper objectMapper;

    public MessageReceiver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void processCaseEvent(String message) throws JsonProcessingException {
        final EventInformation caseEventInformation = createEventInformation(message, "Case Event");
    }

    public void processCaseEventDeadLetterQueue(String message) throws JsonProcessingException {
        final EventInformation caseEventDlqInformation = createEventInformation(message,
                "Case Event Dead Letter Queue");
    }

    private EventInformation createEventInformation(String message, String eventName) throws JsonProcessingException {
        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);

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

        return eventInformation;
    }


}
