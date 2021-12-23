package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class EventInformationRequest {

    @JsonUnwrapped
    private final EventInformation eventInformation;

    @JsonUnwrapped
    private final EventInformationMetadata eventInformationMetadata;

    public EventInformationRequest(EventInformation eventInformation,
                                   EventInformationMetadata eventInformationMetadata) {
        this.eventInformation = eventInformation;
        this.eventInformationMetadata = eventInformationMetadata;
    }

    public EventInformation getEventInformation() {
        return eventInformation;
    }

    public EventInformationMetadata getEventInformationMetadata() {
        return eventInformationMetadata;
    }
}
