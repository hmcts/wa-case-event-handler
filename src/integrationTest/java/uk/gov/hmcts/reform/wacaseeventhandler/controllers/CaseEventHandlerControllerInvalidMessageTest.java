package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.request.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
class CaseEventHandlerControllerInvalidMessageTest {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

    public static final String S2S_TOKEN = "Bearer s2s token";
    public static final String TENANT_ID = "ia";
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

        @ParameterizedTest
        @MethodSource("uk.gov.hmcts.reform.wacaseeventhandler.controllers.CaseEventHandlerControllerEndpointTest"
            + "#provideInvalidMessages")
        void should_store_invalid_unprocessable_message_and_return_200_ok(String invalidMessage) throws Exception {
            String messageId = randomMessageId();

            MvcResult result = mockMvc.perform(post("/messages/"
                                                        + messageId
                                                        + "?from_dlq=false&session_id="
                                                        + CASE_REFERENCE)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(invalidMessage))
                .andExpect(status().isCreated())
                .andReturn();

            String content = result.getResponse().getContentAsString();
            assertEquals(201, result.getResponse().getStatus(), content);
            assertNotNull(content, "Content Should not be null");

            CaseEventMessage response = OBJECT_MAPPER.readValue(content, CaseEventMessage.class);
            assertNotNull(response, "Response should not be null");
            assertEquals(messageId, response.getMessageId(), "Valid MessageId should be returned");
            assertNotNull(response.getSequence(), "Valid sequence should be returned");
            assertEquals(CASE_REFERENCE, response.getCaseId(), "Valid CaseId should be returned");
            assertEquals(false, response.getFromDlq(), "Valid FromDlq should be returned");
            assertEquals(MessageState.UNPROCESSABLE, response.getState(), "Valid State should be returned");
            assertNotNull(response.getReceived(), "Valid Received should be returned");
            assertEquals(0, response.getDeliveryCount(), "Valid DeliveryCount should be returned");
            assertEquals(invalidMessage, response.getMessageContent(), "Valid message should be returned");
        }

        @NotNull
        private String randomMessageId() {
            return UUID.randomUUID().toString();
        }

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

    public static Stream<String> provideInvalidMessages() {
        return Stream.of(
            "{\n"
                + "  \"EventInstanceId\" : \"some event instance Id\",\n"
                + "  \"EventTimeStamp\" : \"" + EVENT_TIME_STAMP + "\",\n"
                + "  \"InvalidField\" : \"data\",\n"
                + "  \"CaseTypeId\" : \"asylum\",\n"
                + "  \"EventId\" : \"some event Id\",\n"
                + "  \"NewStateId\" : \"some new state Id\",\n"
                + "  \"UserId\" : \"some user Id\",\n"
                + "  \"MessageProperties\" : {\n"
                + "      \"property1\" : \"test1\"\n"
                + "  }\n"
                + "}",
            "{\n"
                + "  \"EventInstanceId\" : \"some event instance Id\",\n"
                + "  \"CaseId\" : " + CASE_REFERENCE + ",\n"
                + "  \"InvalidField\" : \"data\",\n"
                + "  \"CaseTypeId\" : \"asylum\",\n"
                + "  \"EventId\" : \"some event Id\",\n"
                + "  \"NewStateId\" : \"some new state Id\",\n"
                + "  \"UserId\" : \"some user Id\",\n"
                + "  \"MessageProperties\" : {\n"
                + "      \"property1\" : \"test1\"\n"
                + "  }\n"
                + "}"
        );
    }
}


