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
public final class InitiateEvaluateResponse implements EvaluateResponse {
    private final DmnValue<String> taskId;
    private final DmnValue<String> group;
    private final DmnValue<Integer> delayDuration;
    private final DmnValue<Integer> workingDaysAllowed;
    private final DmnValue<String> name;
    private final DmnValue<String> taskCategory;
    private final Map<String, DmnValue<Boolean>> processCategories = new HashMap<>();

    @JsonCreator
    public InitiateEvaluateResponse(@JsonProperty("taskId") DmnValue<String> taskId,
                                    @JsonProperty("group") DmnValue<String> group,
                                    @JsonProperty("delayDuration") DmnValue<Integer> delayDuration,
                                    @JsonProperty("workingDaysAllowed") DmnValue<Integer> workingDaysAllowed,
                                    @JsonProperty("name") DmnValue<String> name,
                                    @JsonProperty("taskCategory") DmnValue<String> taskCategory) {
        this.taskId = taskId;
        this.group = group;
        this.delayDuration = delayDuration;
        this.workingDaysAllowed = workingDaysAllowed;
        this.name = name;
        this.taskCategory = taskCategory;
    }

    @JsonAnySetter
    public void setProcessCategories(String name, DmnValue<Boolean> value) {
        this.processCategories.put(name, value);
    }

    public Map<String, DmnValue<Boolean>> getProcessCategories() {
        return processCategories;
    }

    public DmnValue<String> getTaskId() {
        return taskId;
    }

    public DmnValue<String> getGroup() {
        return group;
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

    public DmnValue<String> getTaskCategory() {
        return taskCategory;
    }
}
