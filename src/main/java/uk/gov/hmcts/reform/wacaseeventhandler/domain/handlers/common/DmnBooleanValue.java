package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DmnBooleanValue {

    @JsonProperty("value")
    private final boolean value;
    @JsonProperty("type")
    private final String type;

    public DmnBooleanValue(boolean value) {
        this.value = value;
        this.type = "boolean";
    }

    public boolean isValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
