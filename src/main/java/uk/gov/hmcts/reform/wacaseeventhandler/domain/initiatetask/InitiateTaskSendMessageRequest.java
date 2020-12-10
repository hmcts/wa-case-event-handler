package uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskSendMessageRequest;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
public final class InitiateTaskSendMessageRequest extends TaskSendMessageRequest {

    private final DmnStringValue dueDate;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnStringValue jurisdiction;
    private final DmnStringValue caseType;
    private final DmnStringValue caseId;

    public DmnStringValue getDueDate() {
        return dueDate;
    }

    public DmnStringValue getName() {
        return name;
    }

    public DmnStringValue getTaskId() {
        return taskId;
    }

    public DmnStringValue getGroup() {
        return group;
    }

    public DmnStringValue getJurisdiction() {
        return jurisdiction;
    }

    public DmnStringValue getCaseType() {
        return caseType;
    }

    public DmnStringValue getCaseId() {
        return caseId;
    }

    public DmnIntegerValue getWorkingDaysAllowed() {
        return workingDaysAllowed;
    }
}
