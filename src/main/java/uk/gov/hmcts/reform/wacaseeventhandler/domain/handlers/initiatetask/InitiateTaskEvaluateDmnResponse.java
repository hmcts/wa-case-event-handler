package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnResponse;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class InitiateTaskEvaluateDmnResponse extends TaskEvaluateDmnResponse {
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;

    @JsonCreator
    public InitiateTaskEvaluateDmnResponse(@JsonProperty("taskId") DmnStringValue taskId,
                                           @JsonProperty("group") DmnStringValue group,
                                           @JsonProperty("workingDaysAllowed") DmnIntegerValue workingDaysAllowed,
                                           @JsonProperty("name") DmnStringValue name) {
        super();
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
