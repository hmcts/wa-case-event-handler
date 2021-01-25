package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class WarningEvaluateResponse extends EvaluateResponse {

    private final DmnStringValue action;

    @JsonCreator
    public WarningEvaluateResponse(@JsonProperty("action") DmnStringValue action) {
        super();
        this.action = action;
    }

    public DmnStringValue getAction() {
        return action;
    }

}
