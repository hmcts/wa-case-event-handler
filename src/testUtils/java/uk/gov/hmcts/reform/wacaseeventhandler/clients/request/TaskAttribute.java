package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class TaskAttribute {

    private final TaskAttributeDefinition name;
    private final Object value;

    @JsonCreator
    public TaskAttribute(TaskAttributeDefinition name, Object value) {
        this.name = name;
        this.value = value;
    }

    public TaskAttributeDefinition getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }
}
