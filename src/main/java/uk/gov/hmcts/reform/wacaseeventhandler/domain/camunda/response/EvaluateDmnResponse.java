package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
public final class EvaluateDmnResponse<T extends EvaluateResponse> {

    private final List<T> results;

    @JsonCreator
    public EvaluateDmnResponse(@JsonProperty("results") List<T> results) {
        this.results = results;
    }

    public List<T> getResults() {
        return results;
    }
}

