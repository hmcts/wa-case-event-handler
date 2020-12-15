package uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateResponse;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class InitiateEvaluateResponse extends EvaluateResponse {
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;

    @JsonCreator
    public InitiateEvaluateResponse(@JsonProperty("taskId") DmnStringValue taskId,
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
