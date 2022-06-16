package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class TerminateTaskRequest {

    @JsonProperty("terminate_info")
    private final TerminateInfo terminateInfo;

    @JsonCreator
    public TerminateTaskRequest(TerminateInfo terminateInfo) {
        this.terminateInfo = terminateInfo;
    }

    public TerminateInfo getTerminateInfo() {
        return terminateInfo;
    }
}
