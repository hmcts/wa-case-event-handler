package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.SendMessageRequest;

public interface WorkflowApiClient {

    EvaluateDmnResponse<? extends EvaluateResponse> evaluateDmn(
        String key,
        EvaluateDmnRequest<? extends EvaluateRequest> requestParameters
    );

    ResponseEntity<Void> sendMessage(
        SendMessageRequest<? extends ProcessVariables, ? extends CorrelationKeys> sendMessageRequest);

}
