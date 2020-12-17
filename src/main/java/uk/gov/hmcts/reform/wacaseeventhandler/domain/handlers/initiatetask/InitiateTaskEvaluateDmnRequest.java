package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnRequest;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class InitiateTaskEvaluateDmnRequest extends TaskEvaluateDmnRequest {
    private final DmnStringValue eventId;
    private final DmnStringValue postEventState;

    public InitiateTaskEvaluateDmnRequest(DmnStringValue eventId, DmnStringValue postEventState) {
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
