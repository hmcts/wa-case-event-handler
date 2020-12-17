package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class EvaluateDmnRequest<T extends EvaluateRequest> {

    private final T variables;

    public EvaluateDmnRequest(T variables) {
        this.variables = variables;
    }

    public T getVariables() {
        return variables;
    }
}
