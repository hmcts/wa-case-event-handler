package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnResponse;

@ToString
@EqualsAndHashCode(callSuper = true)
public final class CancellationTaskEvaluateDmnResponse extends TaskEvaluateDmnResponse {

    private final DmnStringValue action;
    private final DmnStringValue taskCategories;


    @JsonCreator
    public CancellationTaskEvaluateDmnResponse(@JsonProperty("action") DmnStringValue action,
                                               @JsonProperty("taskCategories") DmnStringValue taskCategories) {
        super();
        this.action = action;
        this.taskCategories = taskCategories;
    }

    public DmnStringValue getAction() {
        return action;
    }

    public DmnStringValue getTaskCategories() {
        return taskCategories;
    }
}
