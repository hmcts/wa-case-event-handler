package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SuppressWarnings("PMD.UseObjectForClearerAPI")
@FeignClient(
    name = "workflow-api",
    url = "${wa-workflow-api.url}"
)
public interface WorkflowApiClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/workflow/message",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    void sendMessage(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                     SendMessageRequest sendMessageRequest);

    @PostMapping(
        value = "/workflow/decision-definition/key/{key}/tenant-id/{tenant-id}/evaluate",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    EvaluateDmnResponse<CancellationEvaluateResponse> evaluateCancellationDmn(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String key,
        @PathVariable("tenant-id") String tenantId,
        EvaluateDmnRequest evaluateDmnRequest
    );

    @PostMapping(
        value = "/workflow/decision-definition/key/{key}/tenant-id/{tenant-id}/evaluate",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    EvaluateDmnResponse<InitiateEvaluateResponse> evaluateInitiationDmn(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String key,
        @PathVariable("tenant-id") String tenantId,
        EvaluateDmnRequest evaluateDmnRequest
    );

}

