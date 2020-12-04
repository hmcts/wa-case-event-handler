package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.web.bind.annotation.PathVariable;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;

@SuppressWarnings("PMD.GenericsNaming")
public interface WaWorkflowApiClient<RequestT, ResponseT> {

    EvaluateDmnResponse<ResponseT> evaluateDmn(
        @PathVariable("key") String key,
        EvaluateDmnRequest<RequestT> requestParameters
    );


}
