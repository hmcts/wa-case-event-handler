package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class Warning {

    private final String warningCode;
    private final String warningText;

    @JsonCreator
    public Warning(@JsonProperty("warningCode") String warningCode, @JsonProperty("warningText") String warningText) {
        this.warningCode = warningCode;
        this.warningText = warningText;
    }

    public String getWarningCode() {
        return warningCode;
    }

    public String getWarningText() {
        return warningText;
    }

}
