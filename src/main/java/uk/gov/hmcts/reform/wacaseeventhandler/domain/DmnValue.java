package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class DmnValue {
    private final String type;

    public DmnValue(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
