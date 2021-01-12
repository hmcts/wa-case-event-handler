package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;

@ToString
@EqualsAndHashCode(callSuper = true)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InitiateProcessVariables extends ProcessVariables {

    private final DmnStringValue dueDate;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnStringValue jurisdiction;
    private final DmnStringValue caseType;
    private final DmnStringValue caseId;
    private final DmnStringValue taskCategory;

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

    public DmnStringValue getTaskCategory() {
        return taskCategory;
    }
}
