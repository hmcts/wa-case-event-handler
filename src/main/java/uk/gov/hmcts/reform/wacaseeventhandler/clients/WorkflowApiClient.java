package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.TaskSendMessageRequest;

public interface WorkflowApiClient {

    EvaluateDmnResponse<? extends TaskEvaluateDmnResponse> evaluateDmn(
        String key,
        EvaluateDmnRequest<? extends TaskEvaluateDmnRequest> requestParameters
    );

    ResponseEntity<Void> sendMessage(SendMessageRequest<? extends TaskSendMessageRequest> sendMessageRequest);

}
