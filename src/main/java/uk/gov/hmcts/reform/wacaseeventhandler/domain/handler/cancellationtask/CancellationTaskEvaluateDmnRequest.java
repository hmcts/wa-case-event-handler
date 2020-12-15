package uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnRequest;

@ToString
@EqualsAndHashCode(callSuper = true)
public final class CancellationTaskEvaluateDmnRequest extends TaskEvaluateDmnRequest {
    private final DmnStringValue eventId;
    private final DmnStringValue postEventState;
    private final DmnStringValue previousStateId;

    public CancellationTaskEvaluateDmnRequest(DmnStringValue eventId,
                                              DmnStringValue postEventState,
                                              DmnStringValue previousStateId) {
        super();
        this.eventId = eventId;
        this.postEventState = postEventState;
        this.previousStateId = previousStateId;
    }

    public DmnStringValue getEventId() {
        return eventId;
    }

    public DmnStringValue getPostEventState() {
        return postEventState;
    }

    public DmnStringValue getPreviousStateId() {
        return previousStateId;
    }
}
