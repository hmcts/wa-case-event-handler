package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DmnIntegerValue {
    private final String value;
    private final String type;

    public DmnIntegerValue(String value) {
        this.value = value;
        this.type = "Integer";
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }
}
