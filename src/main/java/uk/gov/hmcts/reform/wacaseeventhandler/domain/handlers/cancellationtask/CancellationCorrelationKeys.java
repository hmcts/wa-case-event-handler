package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;

@EqualsAndHashCode(callSuper = true)
@ToString
@Builder
public class CancellationCorrelationKeys extends CorrelationKeys {

    private final DmnStringValue taskCategory;
    private final DmnStringValue caseId;

    public DmnStringValue getTaskCategory() {
        return taskCategory;
    }

    public DmnStringValue getCaseId() {
        return caseId;
    }
}
