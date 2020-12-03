package uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;

@ToString
@EqualsAndHashCode
public final class InitiateTaskDmnRequest {
    private final DmnStringValue eventId;
    private final DmnStringValue postEventState;

    public InitiateTaskDmnRequest(DmnStringValue eventId, DmnStringValue postEventState) {
        this.eventId = eventId;
        this.postEventState = postEventState;
    }

    public DmnStringValue getEventId() {
        return eventId;
    }

    public DmnStringValue getPostEventState() {
        return postEventState;
    }
}
