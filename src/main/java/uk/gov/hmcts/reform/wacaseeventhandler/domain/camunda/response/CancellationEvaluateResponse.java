package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.util.HashMap;
import java.util.Map;

@ToString
@Builder
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class CancellationEvaluateResponse implements EvaluateResponse {

    private final DmnValue<String> action;
    private final DmnValue<String> taskCategories;
    private final Map<String, DmnValue<?>> processCategories = new HashMap<>();

    @JsonCreator
    public CancellationEvaluateResponse(@JsonProperty("action") DmnValue<String> action,
                                        @JsonProperty("TaskCategories") DmnValue<String> taskCategories) {
        this.action = action;
        this.taskCategories = taskCategories;
    }

    @JsonAnySetter
    public void setProcessCategories(String name, DmnValue<?> value) {
        this.processCategories.put(name, value);
    }

    public Map<String, DmnValue<?>> getProcessCategories() {
        return processCategories;
    }

    public DmnValue<String> getAction() {
        return action;
    }

    public DmnValue<String> getTaskCategories() {
        return taskCategories;
    }
}
