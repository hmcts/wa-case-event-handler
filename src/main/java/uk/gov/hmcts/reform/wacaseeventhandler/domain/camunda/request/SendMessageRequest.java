package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.util.Map;

@EqualsAndHashCode
@ToString
@Builder
public class SendMessageRequest {
    private final String messageName;
    private final Map<String, DmnValue<?>> processVariables;
    private final Map<String, DmnValue<?>> correlationKeys;
    private final boolean all;
    private final String tenantId;

    public SendMessageRequest(String messageName,
                              Map<String, DmnValue<?>> processVariables,
                              Map<String, DmnValue<?>> correlationKeys,
                              boolean all,
                              String tenantId) {
        this.messageName = messageName;
        this.processVariables = processVariables;
        this.correlationKeys = correlationKeys;
        this.all = all;
        this.tenantId = tenantId;
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

    public String getTenantId() {
        return tenantId;
    }
}
