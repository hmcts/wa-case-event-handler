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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.CorrelationKeys;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.ProcessVariables;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.SendMessageRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
 class WorkflowApiClientToWarnTaskTest {

    public static final String HTTP_WORKFLOW_API_URL = "http://workflowApiUrl";
    public static final String TABLE_KEY = "someTableKey";
    public static final String TENANT_ID = "ia";
    public static final String BEARER_S_2_S_TOKEN = "some Bearer s2s token";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private AuthTokenGenerator authTokenGenerator;

    private WorkflowApiClientToWarnTask client;

    @BeforeEach
    void setUp() {
        client = new WorkflowApiClientToWarnTask(
            restTemplate,
            authTokenGenerator,
            HTTP_WORKFLOW_API_URL
        );
    }

    @Test
    void evaluateDmn() {
        when(authTokenGenerator.generate()).thenReturn(BEARER_S_2_S_TOKEN);

        when(restTemplate.exchange(
            getExpectedUrl(),
            HttpMethod.POST,
            getExpectedEntity(),
            new ParameterizedTypeReference<EvaluateDmnResponse<WarningResponse>>() {
            }
        ))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        EvaluateDmnResponse<WarningResponse> actualResponse = client.evaluateDmn(
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

    private HttpEntity<SendMessageRequest<? extends ProcessVariables,
        ? extends CorrelationKeys>> getExpectedSendEntity() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("ServiceAuthorization", BEARER_S_2_S_TOKEN);

        return new HttpEntity<>(new SendMessageRequest<>("warnTask",null,
                                                         null, false), headers);
    }

    private String getExpectedSendMessageUrl() {
        return String.format(
            "%s/workflow/message",
            HTTP_WORKFLOW_API_URL
        );
    }

    @Test
    void sendMessage() {
        when(authTokenGenerator.generate()).thenReturn(BEARER_S_2_S_TOKEN);

        when(restTemplate.exchange(
            getExpectedSendMessageUrl(),
            HttpMethod.POST,
            getExpectedSendEntity(),
            Void.class
        ))
            .thenReturn(new ResponseEntity<>(HttpStatus.NO_CONTENT));

        ResponseEntity<Void> actualResponse = client.sendMessage(
            new SendMessageRequest<>(
                "warnTask",
                null,
                null, false
            )
        );

        assertThat(actualResponse.getStatusCode().is2xxSuccessful());
    }

}

