package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.GenericsNaming")
public final class EvaluateDmnResponse<ResponseT> {

    private final ResponseT results;

    @JsonCreator
    public EvaluateDmnResponse(@JsonProperty("results") ResponseT results) {
        this.results = results;
    }

    public ResponseT getResults() {
        return results;
    }
}

