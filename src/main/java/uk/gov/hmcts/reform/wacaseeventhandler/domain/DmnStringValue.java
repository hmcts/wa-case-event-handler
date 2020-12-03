package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class DmnStringValue {
    private final String value;
    private final String type;

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
