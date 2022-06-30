package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.TaskOperationRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@SuppressWarnings("PMD.UseObjectForClearerAPI")
@FeignClient(
    name = "task-management-api",
    url = "${wa-task-management-api.url}"
)
public interface TaskManagementApiClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @PostMapping(
        value = "/task/operation",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    void performOperation(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                          @RequestBody TaskOperationRequest taskOperationRequest);

}

