package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode
@ToString
public class InitiateTaskRequest {

    private final InitiateTaskOperation operation;
    @JsonProperty("task_attributes")
    private final List<TaskAttribute> taskAttributes;

    @JsonCreator
    public InitiateTaskRequest(InitiateTaskOperation operation, List<TaskAttribute> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public InitiateTaskOperation getOperation() {
        return operation;
    }

    public List<TaskAttribute> getTaskAttributes() {
        return taskAttributes;
    }
}
