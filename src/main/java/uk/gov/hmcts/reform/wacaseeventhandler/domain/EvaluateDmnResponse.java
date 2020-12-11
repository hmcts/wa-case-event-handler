package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.GenericsNaming")
public final class EvaluateDmnResponse<ResponseT> {

    private final List<ResponseT> results;

    @JsonCreator
    public EvaluateDmnResponse(@JsonProperty("results") List<ResponseT> results) {
        this.results = results;
    }

    public List<ResponseT> getResults() {
        return results;
    }
}

