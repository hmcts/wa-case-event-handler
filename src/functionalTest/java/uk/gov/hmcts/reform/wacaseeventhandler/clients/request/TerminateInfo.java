package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class TerminateInfo {

    @JsonProperty("terminate_reason")
    private final String terminateReason;

    @JsonCreator
    public TerminateInfo(String terminateReason) {
        this.terminateReason = terminateReason;
    }

    public String getTerminateReason() {
        return terminateReason;
    }
}
