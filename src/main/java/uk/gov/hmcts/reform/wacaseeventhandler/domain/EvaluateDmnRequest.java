package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import java.util.Map;

public final class EvaluateDmnRequest {

    private final Map<String, DmnValue> variables;

    public EvaluateDmnRequest(Map<String, DmnValue> variables) {
        this.variables = variables;
    }

    public Map<String, DmnValue> getVariables() {
        return variables;
    }
}
