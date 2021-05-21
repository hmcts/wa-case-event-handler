package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;


@SuppressWarnings("unchecked")
@ActiveProfiles({"local"})
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CaseEventHandlerControllerEndPointTest {

    public static final String S2S_TOKEN = "Bearer s2s token";
    public static final String TENANT_ID = "ia";
    public static final String INITIATE_DMN_TABLE = "wa-task-initiation-ia-asylum";
    public static final String CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private WorkflowApiClient workflowApiClient;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        mockApiCallsTemplate();
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

    private void mockApiCallsTemplate() {
        mockInitiateHandler();
        mockCancellationHandler();
        mockWarningHandler();
        mockWarningHandlerWithFalse();
    }

    private void mockCancellationHandler() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("some action"),
            dmnStringValue("some category")
        ));

        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse = new EvaluateDmnResponse<>(results);

        doReturn(cancellationResponse).when(workflowApiClient).evaluateCancellationDmn(
            eq(S2S_TOKEN),
            eq(CANCELLATION_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));

    }

    private void mockWarningHandler() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("some action"),
            dmnStringValue("some category")
        ));

        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse = new EvaluateDmnResponse<>(results);

        doReturn(cancellationResponse).when(workflowApiClient).evaluateCancellationDmn(
            eq(S2S_TOKEN),
            eq(CANCELLATION_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));
    }

    private void mockWarningHandlerWithFalse() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Warn"),
            dmnStringValue("some task cat")
        ));
        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse = new EvaluateDmnResponse<>(results);

        doReturn(cancellationResponse).when(workflowApiClient).evaluateCancellationDmn(
            eq(S2S_TOKEN),
            eq(CANCELLATION_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));
    }

    private EvaluateDmnResponse<InitiateEvaluateResponse> mockInitiateHandler() {

        EvaluateDmnResponse<InitiateEvaluateResponse> response = InitiateTaskHelper.buildInitiateTaskDmnResponse();

        doReturn(response).when(workflowApiClient).evaluateInitiationDmn(
            eq(S2S_TOKEN),
            eq(INITIATE_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));

        return response;
    }

    private static Stream<Scenario> scenarioProvider() {
        EventInformation validEventInformation = getEventInformation(null);

        Scenario validEventInformationScenario200 = Scenario.builder()
            .eventInformation(validEventInformation)
            .expectedStatus(HttpStatus.NO_CONTENT.value())
            .build();

        Scenario validEventWithAdditionalDataScenario200 = Scenario.builder()
            .eventInformation(validAdditionalData())
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
            validEventWithAdditionalDataScenario200,
            mandatoryFieldCannotBeNullScenario400,
            mandatoryFieldCannotBeEmptyScenario400
        );
    }

    private static EventInformation getEventInformation(AdditionalData additionalData) {
        EventInformation validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(LocalDateTime.now())
            .caseId("some case reference")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .additionalData(additionalData)
            .build();
        return validEventInformation;
    }

    private static EventInformation validAdditionalData() {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("directionDueDate", "2021-04-06"),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getEventInformation(additionalData);
    }

    @Builder
    private static class Scenario {
        EventInformation eventInformation;
        int expectedStatus;
    }
}

