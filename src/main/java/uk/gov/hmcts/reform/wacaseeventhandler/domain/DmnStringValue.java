package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
public final class DmnStringValue extends DmnValue {
    private final String value;

    public DmnStringValue(String value) {
        super("String");
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
