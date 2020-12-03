package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DmnIntegerValue {
    private final Integer value;
    private final String type;

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
