package uk.gov.hmcts.reform.wacaseeventhandler.entities.camunda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class CamundaVariable {
    private Object value;
    private String type;

    private CamundaVariable() {
        //Hidden constructor
    }

    public CamundaVariable(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
