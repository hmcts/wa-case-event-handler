package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskSendMessageRequest;

@SuppressWarnings("PMD.GenericsNaming")
public interface WorkflowApiClient {

    EvaluateDmnResponse<? extends TaskEvaluateDmnResponse> evaluateDmn(
        String key,
        EvaluateDmnRequest<? extends TaskEvaluateDmnRequest> requestParameters
    );

    ResponseEntity<Void> sendMessage(SendMessageRequest<? extends TaskSendMessageRequest> sendMessageRequest);

}
