package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.GenericsNaming")
public final class SendMessageRequest<RequestT> {

    private final String messageName;
    private final RequestT processVariables;

    public SendMessageRequest(String messageName, RequestT processVariables) {
        this.messageName = messageName;
        this.processVariables = processVariables;
    }

    public String getMessageName() {
        return messageName;
    }

    public RequestT getProcessVariables() {
        return processVariables;
    }

}
