package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;

@ActiveProfiles({"local"})
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CaseEventHandlerControllerEndPointTest {

    public static final String S2S_TOKEN = "Bearer s2s token";
    public static final String DMN_TABLE = "wa-task-initiation-ia-asylum";
    public static final String TENANT_ID = "ia";
    public static final String INITIATE_DMN_TABLE = "wa-task-initiation-ia-asylum";
    public static final String CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private RestTemplate restTemplate;

    @Value("${wa-workflow-api.url}")
    private String workflowApiUrl;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        mockRestTemplate();
    }

    private void mockRestTemplate() {
        mockInitiateHandler();
        mockCancellationHandler();
        mockWarningHandler();
    }

    private void mockCancellationHandler() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            new DmnStringValue("some action"),
            new DmnStringValue("some category")
        ));
        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse =
            new EvaluateDmnResponse<>(results);

        ResponseEntity<EvaluateDmnResponse<CancellationEvaluateResponse>> responseEntity =
            new ResponseEntity<>(cancellationResponse, HttpStatus.OK);

        String cancellationEvaluateUrl = String.format(
            "%s/workflow/decision-definition/key/%s/tenant-id/%s/evaluate",
            workflowApiUrl,
            CANCELLATION_DMN_TABLE,
            TENANT_ID
        );
        Mockito.when(restTemplate.exchange(
            eq(cancellationEvaluateUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.<HttpEntity<List<HttpHeaders>>>any(),
            ArgumentMatchers
                .<ParameterizedTypeReference<EvaluateDmnResponse<CancellationEvaluateResponse>>>any())
        ).thenReturn(responseEntity);
    }


    private void mockWarningHandler() {
        List<WarningEvaluateResponse> results = List.of(new WarningEvaluateResponse(
            new DmnStringValue("some action")
        ));
        EvaluateDmnResponse<WarningEvaluateResponse> cancellationResponse =
            new EvaluateDmnResponse<>(results);

        ResponseEntity<EvaluateDmnResponse<WarningEvaluateResponse>> responseEntity =
            new ResponseEntity<>(cancellationResponse, HttpStatus.OK);

        String cancellationEvaluateUrl = String.format(
            "%s/workflow/decision-definition/key/%s/evaluate",
            workflowApiUrl,
            CANCELLATION_DMN_TABLE
        );
        Mockito.when(restTemplate.exchange(
            eq(cancellationEvaluateUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.<HttpEntity<List<HttpHeaders>>>any(),
            ArgumentMatchers
                .<ParameterizedTypeReference<EvaluateDmnResponse<WarningEvaluateResponse>>>any())
        ).thenReturn(responseEntity);
    }

    private ResponseEntity<EvaluateDmnResponse<InitiateEvaluateResponse>> mockInitiateHandler() {
        ResponseEntity<EvaluateDmnResponse<InitiateEvaluateResponse>> responseEntity =
            new ResponseEntity<>(
                InitiateTaskHelper.buildInitiateTaskDmnResponse(),
                HttpStatus.OK
            );

        String initiateEvaluateUrl = String.format(
            "%s/workflow/decision-definition/key/%s/tenant-id/%s/evaluate",
            workflowApiUrl,
            INITIATE_DMN_TABLE,
            TENANT_ID
        );
        Mockito.when(restTemplate.exchange(
            eq(initiateEvaluateUrl),
            eq(HttpMethod.POST),
            ArgumentMatchers.<HttpEntity<List<HttpHeaders>>>any(),
            ArgumentMatchers.<ParameterizedTypeReference<EvaluateDmnResponse<InitiateEvaluateResponse>>>any())
        ).thenReturn(responseEntity);
        return responseEntity;
    }

    @ParameterizedTest
    @MethodSource(value = "scenarioProvider")
    void given_message_then_return_expected_status_code(Scenario scenario) throws Exception {

        mockMvc.perform(post("/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(scenario.eventInformation)))
            .andDo(print())
            .andExpect(status().is(scenario.expectedStatus));
    }

    private static Stream<Scenario> scenarioProvider() {
        EventInformation validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(LocalDateTime.now())
            .caseId("some case reference")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario validEventInformationScenario200 = Scenario.builder()
            .eventInformation(validEventInformation)
            .expectedStatus(HttpStatus.NO_CONTENT.value())
            .build();

        EventInformation invalidEventInformationBecauseMandatoryFieldCannotBeNull = EventInformation.builder()
            .eventInstanceId(null)
            .caseId("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario mandatoryFieldCannotBeNullScenario400 = Scenario.builder()
            .eventInformation(invalidEventInformationBecauseMandatoryFieldCannotBeNull)
            .expectedStatus(HttpStatus.BAD_REQUEST.value())
            .build();

        EventInformation invalidEventInformationBecauseMandatoryFieldCannotBeEmpty = EventInformation.builder()
            .eventInstanceId("")
            .caseId("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario mandatoryFieldCannotBeEmptyScenario400 = Scenario.builder()
            .eventInformation(invalidEventInformationBecauseMandatoryFieldCannotBeEmpty)
            .expectedStatus(HttpStatus.BAD_REQUEST.value())
            .build();


        return Stream.of(
            validEventInformationScenario200,
            mandatoryFieldCannotBeNullScenario400,
            mandatoryFieldCannotBeEmptyScenario400
        );
    }

    @Builder
    private static class Scenario {
        EventInformation eventInformation;
        int expectedStatus;
    }

}

