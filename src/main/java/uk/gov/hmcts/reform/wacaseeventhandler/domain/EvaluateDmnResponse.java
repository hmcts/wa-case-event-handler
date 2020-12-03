package uk.gov.hmcts.reform.wacaseeventhandler.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.GenericsNaming")
public final class EvaluateDmnResponse<ResponseT> {

    private final ResponseT result;

    public EvaluateDmnResponse(ResponseT result) {
        this.result = result;
    }

    public ResponseT getResult() {
        return result;
    }
}

