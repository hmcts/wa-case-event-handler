package uk.gov.hmcts.reform.wacaseeventhandler.clients.request;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;

import java.util.Map;

@EqualsAndHashCode
@ToString
public class AddProcessVariableRequest {
    private final Map<String, DmnValue<String>> modifications;

    public AddProcessVariableRequest(Map<String, DmnValue<String>> modifications) {
        this.modifications = modifications;
    }

    public Map<String, DmnValue<String>> getModifications() {
        return modifications;
    }
}
