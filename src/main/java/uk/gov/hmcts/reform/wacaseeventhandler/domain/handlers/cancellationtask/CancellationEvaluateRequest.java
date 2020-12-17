package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;

import javax.validation.constraints.NotNull;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class CancellationEvaluateRequest extends EvaluateRequest {
    @NotNull
    private final DmnStringValue event;
    @NotNull
    private final DmnStringValue state;
    @NotNull
    private final DmnStringValue fromState;

    public CancellationEvaluateRequest(DmnStringValue event,
                                       DmnStringValue state,
                                       DmnStringValue fromState) {
        super();
        this.event = event;
        this.state = state;
        this.fromState = fromState;
    }

    public DmnStringValue getEvent() {
        return event;
    }

    public DmnStringValue getState() {
        return state;
    }

    public DmnStringValue getFromState() {
        return fromState;
    }
}
