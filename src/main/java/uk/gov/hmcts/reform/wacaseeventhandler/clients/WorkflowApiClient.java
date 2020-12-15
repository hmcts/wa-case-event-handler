package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskSendMessageRequest;

public interface WorkflowApiClient {

    EvaluateDmnResponse<? extends TaskEvaluateDmnResponse> evaluateDmn(
        String key,
        EvaluateDmnRequest<? extends TaskEvaluateDmnRequest> requestParameters
    );

    ResponseEntity<Void> sendMessage(SendMessageRequest<? extends TaskSendMessageRequest> sendMessageRequest);

}
