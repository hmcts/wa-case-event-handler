package uk.gov.hmcts.reform.wacaseeventhandler;

import io.restassured.http.Header;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;

import java.util.HashMap;
import java.util.Map;

public class EvaluateDmnCallFromCaseEventHandlerTest extends SpringBootFunctionalBaseTest {

    private static final String ENDPOINT_BEING_TESTED =
        "/workflow/decision-definition/key/%s/tenant-id/%s/evaluate";
    private Header authenticationHeaders;

    @Autowired
    private AuthTokenGenerator serviceAuthGenerator;

    @Autowired
    private WorkflowApiClient workflowApiClient;

    @Before
    public void setUp() {
        authenticationHeaders = authorizationHeadersProvider.getAuthorizationHeaders();
    }

    @Test
    public void should_evaluate_and_return_dmn_results_for_wa() {
        Map<String, Object> appealMap = new HashMap<>();
        appealMap.put("caseTypeOfApplication", "C100");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("Data", appealMap);
        EvaluateDmnRequest evaluateDmnRequest = new EvaluateDmnRequest(
            Map.of(
                "eventId", DmnValue.dmnStringValue("hmcCaseUpdDecOutcome"),
                "postEventState", DmnValue.dmnStringValue("DECISION_OUTCOME"),
                "AdditionalData", DmnValue.dmnMapValue(dataMap)
            ));

        EvaluateDmnResponse<InitiateEvaluateResponse> response = workflowApiClient.evaluateInitiationDmn(
            serviceAuthGenerator.generate(),
            PRIVATE_LAW_TASK_INITIATION_WA_ASYLUM,
            TENANT_ID_WA,
            evaluateDmnRequest
        );

        System.out.println(response);
    }
}
