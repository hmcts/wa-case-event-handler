package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class InitiateTaskRequest {

    private final InitiateTaskOperation operation;
    private final Map<String, Object> taskAttributes;

    @JsonCreator
    public InitiateTaskRequest(InitiateTaskOperation operation, Map<String, Object> taskAttributes) {
        this.operation = operation;
        this.taskAttributes = taskAttributes;
    }

    public InitiateTaskOperation getOperation() {
        return operation;
    }

    public Map<String, Object> getTaskAttributes() {
        return taskAttributes;
    }
}
