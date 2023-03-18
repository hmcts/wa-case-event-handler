package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilObject;

@ToString
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public final class InitiateEvaluateResponse implements EvaluateResponse {
    private final DmnValue<String> taskId;
    private final DmnValue<Integer> delayDuration;
    private final DmnValue<Integer> workingDaysAllowed;
    private final DmnValue<String> name;
    private final DmnValue<String> taskCategory;
    private final DmnValue<String> processCategories;
    private final DmnValue<DelayUntilObject> delayUntil;

    @JsonCreator
    public InitiateEvaluateResponse(@JsonProperty("taskId") DmnValue<String> taskId,
                                    @JsonProperty("delayDuration") DmnValue<Integer> delayDuration,
                                    @JsonProperty("workingDaysAllowed") DmnValue<Integer> workingDaysAllowed,
                                    @JsonProperty("name") DmnValue<String> name,
                                    @JsonProperty("taskCategory") DmnValue<String> taskCategory,
                                    @JsonProperty("processCategories") DmnValue<String> processCategories,
                                    @JsonProperty("delayUntil") DmnValue<DelayUntilObject> delayUntil) {
        this.taskId = taskId;
        this.delayDuration = delayDuration;
        this.workingDaysAllowed = workingDaysAllowed;
        this.name = name;
        this.taskCategory = taskCategory;
        this.processCategories = processCategories;
        this.delayUntil = delayUntil;
    }

    public DmnValue<String> getTaskId() {
        return taskId;
    }

    public DmnValue<Integer> getDelayDuration() {
        return delayDuration;
    }

    public DmnValue<Integer> getWorkingDaysAllowed() {
        return workingDaysAllowed;
    }

    public DmnValue<String> getName() {
        return name;
    }

    public DmnValue<String> getProcessCategories() {
        return processCategories;
    }

    public DmnValue<DelayUntilObject> getDelayUntil() {
        return delayUntil;
    }

    /**
     * This method is deprecated and should not be used in new implementations.
     *
     * @return task categories
     * @deprecated part of the old implementation with no support for multiple categories
     */
    @Deprecated(since = "1.1")
    public DmnValue<String> getTaskCategory() {
        return taskCategory;
    }
}
