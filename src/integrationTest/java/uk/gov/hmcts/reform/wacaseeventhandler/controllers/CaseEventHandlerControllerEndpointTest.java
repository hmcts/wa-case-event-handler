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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;


@SuppressWarnings("unchecked")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
class CaseEventHandlerControllerEndpointTest {

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
    void event_information_should_succeed_and_return_204_no_categories() throws Exception {
        mockInitiateHandlerResponseWithNoCategories();

        EventInformation validEventInformation = getBaseEventInformation(null);


        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void case_event_message_should_be_stored_and_return_204_no_content() throws Exception {

        EventInformation validEventInformation = getBaseEventInformation(null);
        String messageId = "123";

        mockMvc.perform(post("/messages/" + messageId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_should_succeed_and_return_204_with_single_category() throws Exception {
        mockInitiateHandlerResponseWithSingleCategory();

        EventInformation validEventInformation = getBaseEventInformation(null);


        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_should_succeed_and_return_204_with_multiple_category() throws Exception {
        mockInitiateHandlerResponseWithMultipleCategories();

        EventInformation validEventInformation = getBaseEventInformation(null);

        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))

            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_with_additional_data_should_succeed_and_return_204_no_categories() throws Exception {
        mockInitiateHandlerResponseWithNoCategories();

        EventInformation validEventInformation = getBaseEventInformationWithAdditionalData();


        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_with_additional_data_should_succeed_and_return_204_with_single_category() throws Exception {
        mockInitiateHandlerResponseWithSingleCategory();

        EventInformation validEventInformation = getBaseEventInformationWithAdditionalData();


        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
            .andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void event_information_with_additional_data_should_succeed_and_return_204_with_multiple_category()
        throws Exception {
        mockInitiateHandlerResponseWithMultipleCategories();

        EventInformation validEventInformation = getBaseEventInformationWithAdditionalData();

        mockMvc.perform(post("/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validEventInformation)))
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

            .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
    }

    private void setUpMocks() {
        mockCancellationHandler();
        mockWarningHandler();
        mockWarningHandlerWithMultipleCategories();
    }

    private void mockCancellationHandler() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Cancel"), null, null,
            null,
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
            dmnStringValue("Warn"), null, null,
            null,
            dmnStringValue("some category")
        ));

        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse = new EvaluateDmnResponse<>(results);

        doReturn(cancellationResponse).when(workflowApiClient).evaluateCancellationDmn(
            eq(S2S_TOKEN),
            eq(CANCELLATION_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));
    }

    private void mockWarningHandlerWithMultipleCategories() {
        List<CancellationEvaluateResponse> results = List.of(new CancellationEvaluateResponse(
            dmnStringValue("Warn"), null, null,
            null,
            dmnStringValue("some category, some other category")
        ));
        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationResponse = new EvaluateDmnResponse<>(results);

        doReturn(cancellationResponse).when(workflowApiClient).evaluateCancellationDmn(
            eq(S2S_TOKEN),
            eq(CANCELLATION_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));
    }

    private EvaluateDmnResponse<InitiateEvaluateResponse> mockInitiateHandlerResponseWithNoCategories() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("processApplication"))
            .group(dmnStringValue("TCW"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .name(dmnStringValue("Process Application"))
            .build();

        EvaluateDmnResponse<InitiateEvaluateResponse> response = new EvaluateDmnResponse<>(List.of(result));

        doReturn(response).when(workflowApiClient).evaluateInitiationDmn(
            eq(S2S_TOKEN),
            eq(INITIATE_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));

        return response;
    }

    private EvaluateDmnResponse<InitiateEvaluateResponse> mockInitiateHandlerResponseWithSingleCategory() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("reviewTheAppeal"))
            .group(dmnStringValue("TCW"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .name(dmnStringValue("Review the appeal"))
            .processCategories(dmnStringValue("caseProgression"))
            .build();

        EvaluateDmnResponse<InitiateEvaluateResponse> response = new EvaluateDmnResponse<>(List.of(result));

        doReturn(response).when(workflowApiClient).evaluateInitiationDmn(
            eq(S2S_TOKEN),
            eq(INITIATE_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));

        return response;
    }

    private EvaluateDmnResponse<InitiateEvaluateResponse> mockInitiateHandlerResponseWithMultipleCategories() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("testTaskIdForMultipleCategories"))
            .group(dmnStringValue("TCW"))
            .delayDuration(dmnIntegerValue(2))
            .workingDaysAllowed(dmnIntegerValue(2))
            .name(dmnStringValue("Test task to test multiple categories"))
            .processCategories(dmnStringValue("caseProgression, followUpOverdue"))
            .build();

        EvaluateDmnResponse<InitiateEvaluateResponse> response = new EvaluateDmnResponse<>(List.of(result));

        doReturn(response).when(workflowApiClient).evaluateInitiationDmn(
            eq(S2S_TOKEN),
            eq(INITIATE_DMN_TABLE),
            eq(TENANT_ID),
            any(EvaluateDmnRequest.class));

        return response;
    }

    public static EventInformation getBaseEventInformation(AdditionalData additionalData) {
        return EventInformation.builder()
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
    }

    private static EventInformation getBaseEventInformationWithAdditionalData() {
        Map<String, Object> dataMap = Map.of(
            "lastModifiedDirection", Map.of("dateDue", "2021-04-06"),
            "appealType", "protection"
        );

        AdditionalData additionalData = AdditionalData.builder()
            .data(dataMap)
            .build();

        return getBaseEventInformation(additionalData);
    }

}

