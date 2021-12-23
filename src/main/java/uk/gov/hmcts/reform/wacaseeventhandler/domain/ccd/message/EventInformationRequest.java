package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@Builder
@EqualsAndHashCode
public class EventInformationRequest {

    @JsonUnwrapped
    private EventInformation eventInformation;

    @JsonUnwrapped
    private EventInformationMetadata eventInformationMetadata;

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
