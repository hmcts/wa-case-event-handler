package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import java.util.List;
import java.util.Map;

public final class EvaluateDmnResponse {

    private final List<Map<String, Object>> response;

    public EvaluateDmnResponse(List<Map<String, Object>> response) {
        this.response = response;
    }

    public List<Map<String, Object>> getResponse() {
        return response;
    }
}
