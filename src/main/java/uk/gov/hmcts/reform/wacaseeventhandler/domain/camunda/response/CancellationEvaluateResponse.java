package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

@ToString
@Builder
public final class CancellationEvaluateResponse implements EvaluateResponse {

    private final DmnValue<String> action;
    private final DmnValue<String> processCategories;

    @JsonCreator
    public CancellationEvaluateResponse(@JsonProperty("action") DmnValue<String> action,
                                        @JsonProperty("processCategories") DmnValue<String> processCategories) {
        this.action = action;
        this.processCategories = processCategories;
    }

    public DmnValue<String> getAction() {
        return action;
    }

    public DmnValue<String> getProcessCategories() {
        return processCategories;
    }
}
