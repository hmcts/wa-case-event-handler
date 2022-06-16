package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.request.TerminateTaskRequest;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "task-management-test-api",
    url = "${wa-task-management-api.url}"
)
public interface TaskManagementTestClient {

    String SERVICE_AUTHORIZATION = "ServiceAuthorization";

    @DeleteMapping(value = "/task/{task-id}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    void terminateTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                       @PathVariable("task-id") String taskId,
                       @RequestBody TerminateTaskRequest body);

    @PostMapping(value = "/task/{task-id}",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    @ResponseBody
    void initiateTask(@RequestHeader(SERVICE_AUTHORIZATION) String serviceAuthorisation,
                      @PathVariable("task-id") String taskId,
                      @RequestBody InitiateTaskRequest body);
}
