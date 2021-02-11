package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;

@FeignClient(
    name = "workflow-api",
    url = "${wa-workflow-api.url.url}"
)
@SuppressWarnings("checkstyle:LineLength")
public interface WorkflowApi {
    public static final String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @GetMapping(
        value = "/workflow/decision-definition/key/{key}/tenant-id/{tenant-id}/evaluate",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    EvaluateDmnResponse<? extends EvaluateResponse> evaluateDmnTable(@PathVariable("key") String key,
                                                                     @PathVariable("tenant-id") String tenant,
                                                                     @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                                                                     @RequestBody EvaluateDmnRequest<? extends EvaluateRequest> body
    );

    @PostMapping(
        value = "/workflow/message",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    void sendMessage(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthToken,
                     @RequestBody SendMessageRequest<? extends ProcessVariables, ? extends CorrelationKeys> body);
}
