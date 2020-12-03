package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;

import java.util.List;

@FeignClient(
    name = "wa-workflow-api",
    url = "${wa-workflow-api.url}"
)
@SuppressWarnings("PMD.GenericsNaming")
public interface WaWorkflowApiClient<RequestT,ResponseT> {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/workflow/decision-definition/key/{key}/evaluate",
        produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    List<EvaluateDmnResponse<ResponseT>> evaluateDmn(
        @RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
        @PathVariable("key") String key,
        EvaluateDmnRequest<RequestT> requestParameters
    );

}
