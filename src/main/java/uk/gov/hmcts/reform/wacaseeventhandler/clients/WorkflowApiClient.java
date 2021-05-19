package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;

@SuppressWarnings("PMD.UseObjectForClearerAPI")
@FeignClient(
    name = "workflow-api",
    url = "${wa-workflow-api.url}"
)
public interface WorkflowApiClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/workflow/message",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    void sendMessage(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                     SendMessageRequest sendMessageRequest);

    @PostMapping(
        value = "/workflow/decision-definition/key/{key}/tenant-id/{tenant-id}/evaluate",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    EvaluateDmnResponse<? extends EvaluateResponse> evaluateDmn(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String key,
        @PathVariable("tenant-id") String tenantId,
        EvaluateDmnRequest evaluateDmnRequest
    );

}

