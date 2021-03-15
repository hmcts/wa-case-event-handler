package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class InitiateEvaluateResponse extends EvaluateResponse {
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnIntegerValue delayDuration;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;
    private final DmnStringValue taskCategory;

    @JsonCreator
    public InitiateEvaluateResponse(@JsonProperty("taskId") DmnStringValue taskId,
                                    @JsonProperty("group") DmnStringValue group,
                                    @JsonProperty("delayDuration") DmnIntegerValue delayDuration,
                                    @JsonProperty("workingDaysAllowed") DmnIntegerValue workingDaysAllowed,
                                    @JsonProperty("name") DmnStringValue name,
                                    @JsonProperty("taskCategory") DmnStringValue taskCategory) {
        super();
        this.taskId = taskId;
        this.group = group;
        this.delayDuration = delayDuration;
        this.workingDaysAllowed = workingDaysAllowed;
        this.name = name;
        this.taskCategory = taskCategory;
    }

    public DmnStringValue getTaskId() {
        return taskId;
    }

    public DmnStringValue getGroup() {
        return group;
    }

    public DmnIntegerValue getDelayDuration() {
        return delayDuration;
    }

    public DmnIntegerValue getWorkingDaysAllowed() {
        return workingDaysAllowed;
    }

    public DmnStringValue getName() {
        return name;
    }

    public DmnStringValue getTaskCategory() {
        return taskCategory;
    }
}
