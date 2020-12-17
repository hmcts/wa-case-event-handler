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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateTaskEvaluateDmnResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class WorkflowApiClientToInitiateTaskTest {

    public static final String HTTP_WORKFLOW_API_URL = "http://workflowApiUrl";
    public static final String TABLE_KEY = "someTableKey";
    public static final String BEARER_S_2_S_TOKEN = "some Bearer s2s token";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private WorkflowApiClientToInitiateTask client;

    @BeforeEach
    void setUp() {
        client = new WorkflowApiClientToInitiateTask(
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
            eq(new ParameterizedTypeReference<EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse>>() {
            })
        ))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse> actualResponse = client.evaluateDmn(
            TABLE_KEY,
            new EvaluateDmnRequest<>(InitiateTaskEvaluateDmnRequest.builder().build())
        );

        assertThat(actualResponse).isEqualTo(
            new ResponseEntity<EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse>>(HttpStatus.NO_CONTENT).getBody());

    }

    private HttpEntity<EvaluateDmnRequest<? extends TaskEvaluateDmnRequest>> getExpectedEntity() {
        EvaluateDmnRequest<? extends TaskEvaluateDmnRequest> requestParameters =
            new EvaluateDmnRequest<>(InitiateTaskEvaluateDmnRequest.builder().build());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("ServiceAuthorization", BEARER_S_2_S_TOKEN);

        return new HttpEntity<>(requestParameters, headers);
    }

    private String getExpectedUrl() {
        return String.format(
            "%s/workflow/decision-definition/key/%s/evaluate",
            HTTP_WORKFLOW_API_URL,
            TABLE_KEY
        );
    }

    @Test
    void sendMessage() {
    }
}
