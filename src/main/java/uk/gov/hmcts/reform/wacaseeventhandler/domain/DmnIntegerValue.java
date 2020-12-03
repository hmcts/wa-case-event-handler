package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
public final class DmnIntegerValue extends DmnValue {
    private final Integer value;

    public DmnIntegerValue(Integer value) {
        super("Integer");
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
