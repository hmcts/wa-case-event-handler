package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.CaseEventHandlerControllerEndpointTest.getBaseEventInformation;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;


@SuppressWarnings("unchecked")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
class CaseEventHandlerControllerBackwardsCompatibilityTest {

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
        setUpMocks();
    }

    @Test
    void event_information_should_succeed_and_return_204() throws Exception {
        EventInformation validEventInformation = getBaseEventInformation(null);

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(validEventInformation)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_with_additional_data_should_succeed_and_return_204() throws Exception {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-06"),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        EventInformation validEventInformation = getBaseEventInformation(additionalData);

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(validEventInformation)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void should_return_400_when_mandatory_field_is_null() throws Exception {

        EventInformation partialEventInformation = EventInformation.builder()
            .eventInstanceId(null)
            .caseId("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(partialEventInformation)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    @Test
    void should_return_400_when_mandatory_field_is_empty() throws Exception {

        EventInformation emptyStringEventInformation = EventInformation.builder()
            .eventInstanceId("")
            .caseId("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(emptyStringEventInformation)))
            .andDo(print())
            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    private void setUpMocks() {
        mockInitiateHandler();
        mockCancellationHandler();
        mockWarningHandler();
        mockWarningHandlerWithFalse();
    }

    private void mockCancellationHandler() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("some action"), null, null,
            dmnStringValue("some category"),
            null
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
            dmnStringValue("Code"),
            dmnStringValue("Text"),
            dmnStringValue("some category"),
            null
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
            dmnStringValue("Warn"), null, null,
            dmnStringValue("some task cat"),
            null
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

}

