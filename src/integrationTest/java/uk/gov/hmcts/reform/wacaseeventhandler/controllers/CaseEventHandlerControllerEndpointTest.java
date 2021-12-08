package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    public static final String S2S_TOKEN = "Bearer s2s token";
    public static final String TENANT_ID = "ia";
    public static final String INITIATE_DMN_TABLE = "wa-task-initiation-ia-asylum";
    public static final String CANCELLATION_DMN_TABLE = "wa-task-cancellation-ia-asylum";
    public static final String CASE_REFERENCE = "some case reference";
    public static final LocalDateTime EVENT_TIME_STAMP = LocalDateTime.now();

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

    @Nested
    class CaseEventHandlerControllerPostMessageEndpointTest {

        @Test
        void case_event_message_should_be_stored_and_return_200_ok() throws Exception {

            String messageId = "123";

            MvcResult result = mockMvc.perform(post("/messages/" + messageId)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(getCaseEventMessage()))
                .andExpect(status().isCreated())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertEquals(1, response.getSequence(), "Valid sequence should be returned");
            assertEquals(CASE_REFERENCE, response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(EVENT_TIME_STAMP, response.getEventTimestamp(), "Valid EventTimestamp should be returned");
            assertEquals(false, response.getFromDlq(), "Valid FromDlq should be returned");
            assertEquals(MessageState.NEW, response.getState(), "Valid State should be returned");
            assertEquals(
                "{\"property1\":\"test1\"}",
                response.getMessageProperties().toString(),
                "Valid MessageProperties should be returned"
            );
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(0, response.getDeliveryCount(), "Valid DeliveryCount should be returned");
            assertEquals(null, response.getHoldUntil(), "Valid HoldUntil should be returned");
            assertEquals(0, response.getRetryCount(), "Valid RetryCount should be returned");

        }

        @Test
        void case_event_message_should_be_stored_and_sequence_bumped() throws Exception {

            String messageId1 = "1231";
            MvcResult result1 = mockMvc.perform(post("/messages/" + messageId1)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(getCaseEventMessage()))
                .andExpect(status().isCreated())
                .andReturn();

            String content1 = result1.getResponse().getContentAsString();
            assertEquals(201, result1.getResponse().getStatus(), content1);

            CaseEventMessage response1 = OBJECT_MAPPER.readValue(content1, CaseEventMessage.class);
            assertNotNull(response1, "Response should not be null");
            assertEquals(messageId1, response1.getMessageId(), "Valid MessageId should be returned");
            assertEquals(2, response1.getSequence(), "Valid sequence should be returned");

            String messageId2 = "1232";
            MvcResult result2 = mockMvc.perform(post("/messages/" + messageId2)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(getCaseEventMessage()))
                .andExpect(status().isCreated())
                .andReturn();

            String content2 = result2.getResponse().getContentAsString();
            assertEquals(201, result2.getResponse().getStatus(), content2);

            CaseEventMessage response2 = OBJECT_MAPPER.readValue(content2, CaseEventMessage.class);
            assertNotNull(response2, "Response should not be null");
            assertEquals(messageId2, response2.getMessageId(), "Valid MessageId should be returned");
            assertEquals(3, response2.getSequence(), "Valid sequence should be returned");

        }

        @Test
        void post_case_event_message_should_return_400_when_messageId_has_been_stored_already() throws Exception {

            String messageId1 = "1230";

            mockMvc.perform(post("/messages/" + messageId1)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(getCaseEventMessage()))
                .andExpect(status().isCreated())
                .andReturn();

            mockMvc.perform(post("/messages/" + messageId1)
                                                    .contentType(MediaType.APPLICATION_JSON)
                                                    .content(getCaseEventMessage()))
                .andExpect(status().isBadRequest())
                .andReturn();
        }
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

    public static String getCaseEventMessage() {
        return "{\n"
            + "  \"EventInstanceId\" : \"some event instance Id\",\n"
            + "  \"EventTimeStamp\" : \"" + EVENT_TIME_STAMP + "\",\n"
            + "  \"CaseId\" : \"some case reference\",\n"
            + "  \"JurisdictionId\" : \"ia\",\n"
            + "  \"CaseTypeId\" : \"asylum\",\n"
            + "  \"EventId\" : \"some event Id\",\n"
            + "  \"NewStateId\" : \"some new state Id\",\n"
            + "  \"UserId\" : \"some user Id\",\n"
            + "  \"MessageProperties\" : {\n"
            + "      \"property1\" : \"test1\"\n"
            + "  }\n"
            + "}";
    }
}

