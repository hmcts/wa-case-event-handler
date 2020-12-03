package uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;

@ToString
@EqualsAndHashCode
public final class InitiateTaskDmnResponse {
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;

    @JsonCreator
    public InitiateTaskDmnResponse(@JsonProperty("taskId") DmnStringValue taskId,
                                   @JsonProperty("group") DmnStringValue group,
                                   @JsonProperty("workingDaysAllowed") DmnIntegerValue workingDaysAllowed,
                                   @JsonProperty("name") DmnStringValue name) {
        this.taskId = taskId;
        this.group = group;
        this.workingDaysAllowed = workingDaysAllowed;
        this.name = name;
    }

    public DmnStringValue getTaskId() {
        return taskId;
    }

    public DmnStringValue getGroup() {
        return group;
    }

    public DmnIntegerValue getWorkingDaysAllowed() {
        return workingDaysAllowed;
    }

    public DmnStringValue getName() {
        return name;
    }
}
