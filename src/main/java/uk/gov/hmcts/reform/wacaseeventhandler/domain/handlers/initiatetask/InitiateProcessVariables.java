package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnBooleanValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnIntegerValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;

@Builder
@ToString
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class InitiateProcessVariables extends ProcessVariables {

    private final DmnStringValue idempotencyKey;
    private final DmnStringValue taskState;
    private final DmnStringValue dueDate;
    private final DmnIntegerValue workingDaysAllowed;
    private final DmnStringValue name;
    private final DmnStringValue taskId;
    private final DmnStringValue group;
    private final DmnStringValue jurisdiction;
    private final DmnStringValue caseTypeId;
    private final DmnStringValue caseId;
    private final DmnStringValue delayUntil;
    private final DmnStringValue taskCategory;
    @SuppressWarnings("PMD.LinguisticNaming")
    private final DmnBooleanValue hasWarnings;

    public DmnStringValue getIdempotencyKey() {
        return idempotencyKey;
    }

    public DmnStringValue getTaskState() {
        return taskState;
    }

    public DmnStringValue getDueDate() {
        return dueDate;
    }

    public DmnIntegerValue getWorkingDaysAllowed() {
        return workingDaysAllowed;
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

    public DmnStringValue getCaseTypeId() {
        return caseTypeId;
    }

    public DmnStringValue getCaseId() {
        return caseId;
    }

    public DmnStringValue getDelayUntil() {
        return delayUntil;
    }

    public DmnStringValue getTaskCategory() {
        return taskCategory;
    }

    public DmnBooleanValue getHasWarnings() {
        return hasWarnings;
    }
}
