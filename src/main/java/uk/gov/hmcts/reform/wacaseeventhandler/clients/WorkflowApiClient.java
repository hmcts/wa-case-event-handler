package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;

@SuppressWarnings("PMD.GenericsNaming")
public interface WorkflowApiClient<EvaluateReqT, EvaluateResT, SendMsgRequestT> {

    EvaluateDmnResponse<EvaluateResT> evaluateDmn(
        String key,
        EvaluateDmnRequest<EvaluateReqT> requestParameters
    );

    ResponseEntity<Void> sendMessage(SendMessageRequest<SendMsgRequestT> sendMessageRequest);

}
