package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request;


import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.util.Map;

@EqualsAndHashCode
@ToString
@Builder
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class SendMessageRequest {
    private final String messageName;
    private final Map<String, DmnValue<?>> processVariables;
    private final Map<String, DmnValue<?>> correlationKeys;
    private final boolean all;

    public SendMessageRequest(String messageName,
                              Map<String, DmnValue<?>> processVariables,
                              Map<String, DmnValue<?>> correlationKeys,
                              boolean all) {
        this.messageName = messageName;
        this.processVariables = processVariables;
        this.correlationKeys = correlationKeys;
        this.all = all;
    }

    public String getMessageName() {
        return messageName;
    }

    public Map<String, DmnValue<?>> getProcessVariables() {
        return processVariables;
    }

    public Map<String, DmnValue<?>> getCorrelationKeys() {
        return correlationKeys;
    }

    public boolean isAll() {
        return all;
    }
}
