package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
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
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnIntegerValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
class CaseEventHandlerControllerEndpointTest {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

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

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
        setUpMocks();
    }

    @Nested
    class CaseEventHandlerControllerPostMessageEndpointTest {

        @BeforeEach
        public void setup() {
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
        }

        @Test
        void case_event_message_should_be_stored_and_return_200_ok() throws Exception {

            String messageId = randomMessageId();

            MvcResult result = postMessage(messageId, status().isCreated(), false);

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
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
            assertNull(response.getHoldUntil(), "Valid HoldUntil should be returned");
            assertEquals(0, response.getRetryCount(), "Valid RetryCount should be returned");
        }

        @Test
        void should_get_the_case_event_message_successfully() throws Exception {

            String messageId = randomMessageId();

            MvcResult storedResult = postMessage(messageId, status().isCreated(), false);
            String storedContent = storedResult.getResponse().getContentAsString();

            MvcResult result = mockMvc.perform(get("/messages/" + messageId))
                .andExpect(status().isOk()).andReturn();

            String content = result.getResponse().getContentAsString();
            assertEquals(200, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage stored = OBJECT_MAPPER.readValue(storedContent, CaseEventMessage.class);
            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(stored.getMessageId(), response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
            assertEquals(stored.getCaseId(), response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(stored.getEventTimestamp(), response.getEventTimestamp(), "Should be same EventTimestamp");
            assertEquals(stored.getFromDlq(), response.getFromDlq(), "Valid FromDlq should be returned");
            assertEquals(stored.getState(), response.getState(), "Valid State should be returned");
            assertEquals(
                stored.getMessageProperties().toString(),
                response.getMessageProperties().toString(),
                "Valid MessageProperties should be returned"
            );
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(stored.getDeliveryCount(), response.getDeliveryCount(), "Should be same DeliveryCount");
            assertNull(response.getHoldUntil(), "Valid HoldUntil should be returned");
            assertEquals(stored.getRetryCount(), response.getRetryCount(), "Valid RetryCount should be returned");
        }

        @Test
        void should_return_404_when_get_the_case_event_message_not_found() throws Exception {

            String messageId = randomMessageId();

            MvcResult result = mockMvc.perform(get("/messages/" + messageId))
                .andExpect(status().isNotFound()).andReturn();

            assertEquals(404, result.getResponse().getStatus(), "Should return 404 Not Found");
        }

        @Test
        void case_event_message_should_be_stored_and_sequence_bumped() throws Exception {

            String messageId1 = randomMessageId();
            MvcResult result1 = postMessage(messageId1, status().isCreated(), false);

            String content1 = result1.getResponse().getContentAsString();
            assertEquals(201, result1.getResponse().getStatus(), content1);

            CaseEventMessage response1 = OBJECT_MAPPER.readValue(content1, CaseEventMessage.class);
            assertNotNull(response1, "Response should not be null");
            assertEquals(messageId1, response1.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response1.getSequence(), "Valid sequence should be returned");

            String messageId2 = randomMessageId();
            MvcResult result2 = postMessage(messageId2, status().isCreated(), false);

            String content2 = result2.getResponse().getContentAsString();
            assertEquals(201, result2.getResponse().getStatus(), content2);

            CaseEventMessage response2 = OBJECT_MAPPER.readValue(content2, CaseEventMessage.class);
            assertNotNull(response2, "Response should not be null");
            assertEquals(messageId2, response2.getMessageId(), "Valid MessageId should be returned");
            assertEquals(response1.getSequence() + 1, response2.getSequence(), "Valid sequence should be returned");
        }

        @Test
        void post_case_event_message_should_update_delivery_count_when_messageId_already_stored() throws Exception {

            String messageId1 = randomMessageId();

            postMessage(messageId1, status().isCreated(), false);

            final MvcResult mvcResult = postMessage(messageId1, status().isCreated(), false);

            CaseEventMessage response =
                OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(1, response.getDeliveryCount());

            final MvcResult mvcResult2 = postMessage(messageId1, status().isCreated(), false);

            CaseEventMessage response2 =
                OBJECT_MAPPER.readValue(mvcResult2.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(2, response2.getDeliveryCount());
        }

        @Test
        void post_case_event_message_with_different_content_should_update_delivery_count_when_messageId_already_stored()
            throws Exception {

            String messageId1 = randomMessageId();

            postMessage(messageId1, status().isCreated(), false);

            final MvcResult mvcResult = postMessage(messageId1, status().isCreated(), false);

            CaseEventMessage response =
                OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(1, response.getDeliveryCount());

            final MvcResult mvcResult2 = mockMvc.perform(post("/messages/" + messageId1)
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(getCaseEventMessageWithDifferentContent(CASE_REFERENCE)))
                .andExpect(status().isCreated())
                .andReturn();

            CaseEventMessage response2 =
                OBJECT_MAPPER.readValue(mvcResult2.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(2, response2.getDeliveryCount());
        }

        @Test
        void post_case_event_message_on_dlq_should_update_delivery_count_when_messageId_already_stored()
            throws Exception {

            String messageId1 = randomMessageId();

            postMessage(messageId1, status().isCreated(), true);

            final MvcResult mvcResult = postMessage(messageId1, status().isCreated(), true);

            CaseEventMessage response =
                OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(1, response.getDeliveryCount());

            final MvcResult mvcResult2 = postMessage(messageId1, status().isCreated(), true);

            CaseEventMessage response2 =
                OBJECT_MAPPER.readValue(mvcResult2.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(2, response2.getDeliveryCount());
        }

        @Test
        void post_case_event_duplicate_message_on_dlq_should_update_delivery_count_when_messageId_already_stored()
            throws Exception {

            String messageId1 = randomMessageId();

            postMessage(messageId1, status().isCreated(), true);

            final MvcResult mvcResult = postMessage(messageId1, status().isCreated(), true);

            CaseEventMessage response =
                OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(1, response.getDeliveryCount());

            final MvcResult mvcResult2 = mockMvc.perform(post("/messages/" + messageId1 + "?from_dlq=true")
                     .contentType(MediaType.APPLICATION_JSON)
                     .content(getCaseEventMessageWithDifferentContent(CASE_REFERENCE)))
                .andExpect(status().isCreated())
                .andReturn();

            CaseEventMessage response2 =
                OBJECT_MAPPER.readValue(mvcResult2.getResponse().getContentAsString(), CaseEventMessage.class);
            assertEquals(2, response2.getDeliveryCount());
        }

        @Test
        void dlq_case_event_message_should_be_stored_and_return_200_ok() throws Exception {

            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
            String messageId = randomMessageId();

            MvcResult result = postMessage(messageId, status().isCreated(), true);

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
            assertEquals(CASE_REFERENCE, response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(EVENT_TIME_STAMP, response.getEventTimestamp(), "Valid EventTimestamp should be returned");
            assertEquals(true, response.getFromDlq(), "Valid FromDlq should be returned");
            assertEquals(MessageState.NEW, response.getState(), "Valid State should be returned");
            assertEquals(
                "{\"property1\":\"test1\"}",
                response.getMessageProperties().toString(),
                "Valid MessageProperties should be returned"
            );
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(0, response.getDeliveryCount(), "Valid DeliveryCount should be returned");
            assertNull(response.getHoldUntil(), "Valid HoldUntil should be returned");
            assertEquals(0, response.getRetryCount(), "Valid RetryCount should be returned");
        }

        @Test
        void dlq_case_event_message_should_not_be_stored_and_return_200_ok_when_feature_flag_disabled()
            throws Exception {

            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(false);
            String messageId = randomMessageId();

            MvcResult result = postMessage(messageId, status().isCreated(), true);

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertTrue(StringUtils.isEmpty(content));
        }

        @Test
        void should_store_no_caseId_unprocessable_message_and_return_200_ok() throws Exception {
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
            String messageId = randomMessageId();
            String unprocessableMessage = getCaseEventMessage(null);

            MvcResult result = mockMvc.perform(post("/messages/" + messageId + "?from_dlq=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unprocessableMessage))
                .andExpect(status().isCreated())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
            assertNull(response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(MessageState.UNPROCESSABLE, response.getState(), "Valid State should be returned");
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(0, response.getDeliveryCount(), "Valid DeliveryCount should be returned");
            assertEquals(unprocessableMessage, response.getMessageContent(), "Valid message should be returned");
        }

        @Test
        void should_store_parsing_error_unprocessable_message_and_return_200_ok() throws Exception {
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
            String messageId = randomMessageId();
            String unprocessableMessage = getUnprocessableCaseEventMessage();

            MvcResult result = mockMvc.perform(post("/messages/" + messageId + "?from_dlq=true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unprocessableMessage))
                .andExpect(status().isCreated())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
            assertNull(response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(true, response.getFromDlq(), "Valid FromDlq should be returned");
            assertEquals(MessageState.UNPROCESSABLE, response.getState(), "Valid State should be returned");
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(0, response.getDeliveryCount(), "Valid DeliveryCount should be returned");
            assertEquals(unprocessableMessage, response.getMessageContent(), "Valid message should be returned");
        }

        @NotNull
        private String randomMessageId() {
            return "messageId_" + ThreadLocalRandom.current().nextLong(1000000);
        }

        @NotNull
        private MvcResult postMessage(String messageId, ResultMatcher created, boolean fromDlq) throws Exception {
            return mockMvc.perform(post("/messages/" + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(getCaseEventMessage(CASE_REFERENCE)))
                .andExpect(created)
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

    private void mockInitiateHandlerResponseWithNoCategories() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("processApplication"))
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
    }

    private void mockInitiateHandlerResponseWithSingleCategory() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("reviewTheAppeal"))
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
    }

    private void mockInitiateHandlerResponseWithMultipleCategories() {

        InitiateEvaluateResponse result = InitiateEvaluateResponse.builder()
            .taskId(dmnStringValue("testTaskIdForMultipleCategories"))
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

    public static String getCaseEventMessage(String caseId) {
        return "{\n"
               + "  \"EventInstanceId\" : \"some event instance Id\",\n"
               + "  \"EventTimeStamp\" : \"" + EVENT_TIME_STAMP + "\",\n"
               + (caseId != null ? "  \"CaseId\" : \"" + caseId + "\",\n" : "  \"CaseId\" : null,\n")
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

    public static String getCaseEventMessageWithDifferentContent(String caseId) {
        return "{\n"
            + "  \"EventInstanceId\" : \"another event instance Id\",\n"
            + "  \"EventTimeStamp\" : \"" + EVENT_TIME_STAMP + "\",\n"
            + (caseId != null ? "  \"CaseId\" : \"" + caseId + "\",\n" : "  \"CaseId\" : null,\n")
            + "  \"JurisdictionId\" : \"ia\",\n"
            + "  \"CaseTypeId\" : \"asylum\",\n"
            + "  \"EventId\" : \"another event Id\",\n"
            + "  \"NewStateId\" : \"another new state Id\",\n"
            + "  \"UserId\" : \"another user Id\",\n"
            + "  \"MessageProperties\" : {\n"
            + "      \"property1\" : \"test1\"\n"
            + "  }\n"
            + "}";
    }

    public static String getUnprocessableCaseEventMessage() {
        return "{\n"
               + "  \"EventInstanceId\" : \"some event instance Id\",\n"
               + "  \"EventTimeStamp\" : \"" + EVENT_TIME_STAMP + "\",\n"
               + "  \"CaseId\" : null,\n"
               + "  \"InvalidField\" : \"data\",\n"
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

