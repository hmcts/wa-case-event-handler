package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
public final class CancellationEvaluateResponse implements EvaluateResponse {

    @JsonProperty("action")
    private DmnValue<String> action;
    @JsonProperty("warningCode")
    private DmnValue<String> warningCode;
    @JsonProperty("warningText")
    private DmnValue<String> warningText;
    @JsonProperty("TaskCategories")
    private DmnValue<String> taskCategories;
    @JsonProperty("processCategories")
    private DmnValue<String> processCategories;

    public CancellationEvaluateResponse() {
        //No-op constructor for deserialization
    }

    public CancellationEvaluateResponse(DmnValue<String> action,
                                        DmnValue<String> warningCode,
                                        DmnValue<String> warningText,
                                        DmnValue<String> taskCategories,
                                        DmnValue<String> processCategories) {
        this.action = action;
        this.warningCode = warningCode;
        this.warningText = warningText;
        this.taskCategories = taskCategories;
        this.processCategories = processCategories;

    }

    public DmnValue<String> getProcessCategories() {
        return processCategories;
    }

    public DmnValue<String> getAction() {
        return action;
    }

    /**
     * This method is deprecated and should not be used in new implementations.
     *
     * @return task categories
     * @deprecated part of the old implementation with no support for multiple categories
     */
    @Deprecated(since = "1.1")
    public DmnValue<String> getTaskCategories() {
        return taskCategories;
    }

    public DmnValue<String> getWarningCode() {
        return warningCode;
    }

    public DmnValue<String> getWarningText() {
        return warningText;
    }
}
