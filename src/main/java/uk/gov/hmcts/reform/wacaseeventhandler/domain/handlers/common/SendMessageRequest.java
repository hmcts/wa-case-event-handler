package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public final class SendMessageRequest<T extends ProcessVariables, S extends CorrelationKeys> {

    private final String messageName;
    private final T processVariables;
    private final S correlationKeys;
    private final boolean all;

    public SendMessageRequest(String messageName, T processVariables,
                              S correlationKeys, boolean all) {
        this.messageName = messageName;
        this.processVariables = processVariables;
        this.correlationKeys = correlationKeys;
        this.all = all;
    }

    public String getMessageName() {
        return messageName;
    }

    public T getProcessVariables() {
        return processVariables;
    }

    public S getCorrelationKeys() {
        return correlationKeys;
    }

    public boolean isAll() {
        return all;
    }

}
