package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DmnStringValue {

    @JsonProperty("value")
    private final String value;
    @JsonProperty("type")
    private final String type;

    @JsonCreator
    public DmnStringValue(String value) {
        this.value = value;
        this.type = "String";
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
