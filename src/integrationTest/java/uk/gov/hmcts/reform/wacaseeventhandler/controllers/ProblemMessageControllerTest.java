package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"azure.servicebus.enableASB=true",
    "azure.servicebus.connection-string="
    + "Endpoint=sb://REPLACE_ME/;SharedAccessKeyName=REPLACE_ME;SharedAccessKey=REPLACE_ME",
    "azure.servicebus.topic-name=test",
    "azure.servicebus.subscription-name=test",
    "azure.servicebus.ccd-case-events-subscription-name=test",
    "azure.servicebus.retry-duration=2"})
class ProblemMessageControllerTest {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

    public static final String S2S_TOKEN = "Bearer s2s token";
    public static final String CASE_REFERENCE = "some case reference";
    public static final LocalDateTime EVENT_TIME_STAMP = LocalDateTime.now();
    public static final LocalDateTime OLD_EVENT_TIME_STAMP = LocalDateTime.now().minusHours(2);

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @BeforeEach
    void setUp() {
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);
    }

    @NotNull
    private String randomMessageId() {
        return "messageId_" + ThreadLocalRandom.current().nextLong(1000000);
    }

    @Test
    void should_return_a_ready_message_when_ready_message_timestamp_is_old_than_one_hour() throws Exception {

        String messageId = randomMessageId();

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true).thenReturn(true);

        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(true);

        postAnOldMessage(messageId, status().isCreated(), false);

        await().pollInterval(600, MILLISECONDS)
            .atMost(45, SECONDS)
            .untilAsserted(() -> checkMessagesInState(List.of(messageId), MessageState.READY));

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        List<CaseEventMessage> response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                        new TypeReference<>() {});

        List<MessageState> messageStates = response.stream().map(caseEventMessage1 -> caseEventMessage1.getState())
            .collect(Collectors.toList());
        assertNotNull(mvcResult, "Response should not be null");
        assertTrue(messageStates.contains(MessageState.READY));
    }

    @Test
    void should_return_unprocessable_message_when_case_id_is_null() throws Exception {
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

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        List<CaseEventMessage> response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                        new TypeReference<>() {});
        List<MessageState> messageStates = response.stream().map(caseEventMessage1 -> caseEventMessage1.getState())
            .collect(Collectors.toList());

        assertNotNull(mvcResult, "Response should not be null");
        assertTrue(messageStates.contains(MessageState.UNPROCESSABLE));
    }


    private void checkMessagesInState(List<String> messageIds, MessageState messageState) {
        messageIds.forEach(msgId -> {
            try {
                mockMvc.perform(get("/messages/" + msgId))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{'EventTimestamp':'" + OLD_EVENT_TIME_STAMP + "'}"))
                    .andExpect(content().json("{'State':'" + messageState.name() + "'}"))
                    .andReturn();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

    public static String getOldCaseEventMessage(String caseId) {
        return "{\n"
               + "  \"EventInstanceId\" : \"some event instance Id\",\n"
               + "  \"EventTimeStamp\" : \"" + OLD_EVENT_TIME_STAMP + "\",\n"
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

    @NotNull
    private MvcResult postAnOldMessage(String messageId, ResultMatcher created, boolean fromDlq) throws Exception {
        return mockMvc.perform(post("/messages/" + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"))
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(getOldCaseEventMessage(CASE_REFERENCE)))
            .andExpect(created)
            .andReturn();
    }

    @NotNull
    private MvcResult postAJobRequest(String jobName) throws Exception {
        return mockMvc.perform(post("/messages/jobs/" + jobName)
                                   .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    }
}

