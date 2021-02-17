package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowApiClientToCancelTaskTest {

    public static final String HTTP_WORKFLOW_API_URL = "http://workflowApiUrl";
    public static final String TABLE_KEY = "someTableKey";
    public static final String TENANT_ID = "ia";
    public static final String BEARER_S_2_S_TOKEN = "some Bearer s2s token";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private WorkflowApiClientToCancelTask client;

    @BeforeEach
    void setUp() {
        client = new WorkflowApiClientToCancelTask(
            restTemplate,
            authTokenGenerator,
            HTTP_WORKFLOW_API_URL
        );
    }

    @Test
    void evaluateDmn() {
        when(authTokenGenerator.generate()).thenReturn(BEARER_S_2_S_TOKEN);

        when(restTemplate.exchange(
            eq(getExpectedUrl()),
            eq(HttpMethod.POST),
            eq(getExpectedEntity()),
            eq(new ParameterizedTypeReference<EvaluateDmnResponse<CancellationEvaluateResponse>>() {
            })
        ))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        EvaluateDmnResponse<CancellationEvaluateResponse> actualResponse = client.evaluateDmn(
            TABLE_KEY,
            new EvaluateDmnRequest<>(CancellationEvaluateRequest.builder().build()),
            TENANT_ID
        );

        assertThat(actualResponse).isEqualTo(
            new ResponseEntity<EvaluateDmnResponse<CancellationEvaluateResponse>>(HttpStatus.NO_CONTENT).getBody());

    }

    private HttpEntity<EvaluateDmnRequest<? extends EvaluateRequest>> getExpectedEntity() {
        EvaluateDmnRequest<? extends EvaluateRequest> requestParameters =
            new EvaluateDmnRequest<>(CancellationEvaluateRequest.builder().build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("ServiceAuthorization", BEARER_S_2_S_TOKEN);

        return new HttpEntity<>(requestParameters, headers);
    }

    private String getExpectedUrl() {
        return String.format(
            "%s/workflow/decision-definition/key/%s/tenant-id/%s/evaluate",
            HTTP_WORKFLOW_API_URL,
            TABLE_KEY,
            TENANT_ID
        );
    }

    @Test
    void sendMessage() {
    }
}
