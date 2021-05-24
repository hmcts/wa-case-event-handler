package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

@ToString
@Builder
@SuppressWarnings("PMD.UseConcurrentHashMap")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CancellationEvaluateResponse implements EvaluateResponse {

    @JsonProperty("action")
    private DmnValue<String> action;
    @JsonProperty("TaskCategories")
    private DmnValue<String> taskCategories;
    @JsonProperty("processCategories")
    private DmnValue<String> processCategories;

    public CancellationEvaluateResponse() {
        //No-op constructor for deserialization
    }

    public CancellationEvaluateResponse(DmnValue<String> action,
                                        DmnValue<String> taskCategories,
                                        DmnValue<String> processCategories) {
        this.action = action;
        this.taskCategories = taskCategories;
        this.processCategories = processCategories;

    }

    public DmnValue<String> getProcessCategories() {
        return processCategories;
    }

    public DmnValue<String> getAction() {
        return action;
    }

    public DmnValue<String> getTaskCategories() {
        return taskCategories;
    }
}
