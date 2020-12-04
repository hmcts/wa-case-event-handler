package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DmnIntegerValue {

    @JsonProperty("value")
    private final Integer value;
    @JsonProperty("type")
    private final String type;

    @JsonCreator
    public DmnIntegerValue(Integer value) {
        this.value = value;
        this.type = "Integer";
    }

    public Integer getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
