package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningResponse;

@Service
@Slf4j
public class WorkflowApiClientToWarnTask implements WorkflowApiClient {

    private final RestTemplate restTemplate;
    private final AuthTokenGenerator authTokenGenerator;
    private final String workflowApiUrl;

    public WorkflowApiClientToWarnTask(RestTemplate restTemplate,
                                         AuthTokenGenerator authTokenGenerator,
                                         @Value("${wa-workflow-api.url}") String workflowApiUrl) {
        this.restTemplate = restTemplate;
        this.authTokenGenerator = authTokenGenerator;
        this.workflowApiUrl = workflowApiUrl;
    }

    private HttpHeaders buildHttpHeader() {
        var header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.set("ServiceAuthorization", authTokenGenerator.generate());
        return header;
    }

    @Override
    public EvaluateDmnResponse<WarningResponse>
        evaluateDmn(String key,
                EvaluateDmnRequest<? extends EvaluateRequest> requestParameters,
                String tenantId) {
        return restTemplate.<EvaluateDmnResponse<WarningResponse>>exchange(
            String.format("%s/workflow/decision-definition/key/%s/tenant-id/%s/evaluate", workflowApiUrl, key,tenantId),
            HttpMethod.POST,
            new HttpEntity<>(requestParameters, buildHttpHeader()),
            new ParameterizedTypeReference<>() {
            }
        ).getBody();
    }

    @Override
    public ResponseEntity<Void> sendMessage(
        SendMessageRequest<? extends ProcessVariables, ? extends CorrelationKeys> messageRequest) {
        return restTemplate.exchange(
            String.format("%s/workflow/message", workflowApiUrl),
            HttpMethod.POST,
            new HttpEntity<>(messageRequest, buildHttpHeader()),
            Void.class
        );
    }

}


