package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;

import java.util.List;

@Service
public class WaWorkflowApiClientToInitiateTask
    implements WaWorkflowApiClient<InitiateTaskDmnRequest, InitiateTaskDmnResponse> {

    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;
    private final String workflowApiUrl;

    public WaWorkflowApiClientToInitiateTask(RestTemplate restTemplate,
                                             AuthTokenGenerator authTokenGenerator,
                                             @Value("${wa-workflow-api.url}") String workflowApiUrl) {
        this.restTemplate = restTemplate;
        this.authTokenGenerator = authTokenGenerator;
        this.workflowApiUrl = workflowApiUrl;
    }

    @Override
    public List<EvaluateDmnResponse<InitiateTaskDmnResponse>> evaluateDmn(
        String key,
        EvaluateDmnRequest<InitiateTaskDmnRequest> requestParameters
    ) {

        return restTemplate.exchange(
            String.format("%s/workflow/decision-definition/key/%s/evaluate", workflowApiUrl, key),
            HttpMethod.POST,
            new HttpEntity<>(requestParameters, buildHttpHeaders()),
            new ParameterizedTypeReference<List<EvaluateDmnResponse<InitiateTaskDmnResponse>>>() {}
        ).getBody();
    }

    private HttpHeaders buildHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("ServiceAuthorization", authTokenGenerator.generate());
        return headers;
    }

}
