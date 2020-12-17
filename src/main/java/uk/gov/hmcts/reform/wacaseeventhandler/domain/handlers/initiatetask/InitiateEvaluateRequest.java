package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;

@ToString
@EqualsAndHashCode(callSuper = true)
public final class InitiateEvaluateRequest extends EvaluateRequest {
    private final DmnStringValue eventId;
    private final DmnStringValue postEventState;

    public InitiateEvaluateRequest(DmnStringValue eventId, DmnStringValue postEventState) {
        super();
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
