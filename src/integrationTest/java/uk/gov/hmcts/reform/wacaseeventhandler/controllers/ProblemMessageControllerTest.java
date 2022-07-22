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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
@ActiveProfiles(profiles = {"integration","db"})
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=true",
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
    public static final String MESSAGE = "Response should not be null";
    public static final String MESSAGE_ENDPOINT = "/messages/";
    public static final String ENDPOINT_UNDER_TEST = "/messages/jobs/";


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

    @NotNull
    private String randomCaseId() {
        return "some case id" + ThreadLocalRandom.current().nextLong(1000000);
    }

    @Test
    void should_return_a_ready_message_when_ready_message_timestamp_is_older_than_one_hour() throws Exception {

        String messageId = randomMessageId();

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);

        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(true);
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(65);
        postMessage(messageId, status().isCreated(), false,localDateTime);

        await().pollInterval(600, MILLISECONDS)
            .atMost(45, SECONDS)
            .untilAsserted(() -> checkMessagesInState(List.of(messageId), MessageState.READY));

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        JobResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                       new TypeReference<>() {});

        assertNotNull(mvcResult, MESSAGE);
        assertTrue(response.getMessageIds().contains(messageId));
    }

    @Test
    void should_not_return_ready_when_timestamp_is_less_than_one_hour() throws Exception {

        String messageId = randomMessageId();

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);

        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(true);
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(30);
        postMessage(messageId, status().isCreated(), false,localDateTime);

        await().pollInterval(600, MILLISECONDS)
            .atMost(45, SECONDS)
            .untilAsserted(() -> checkMessagesInState(List.of(messageId), MessageState.READY));

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        JobResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                        new TypeReference<>() {});
        assertFalse(response.getMessageIds().contains(messageId));
    }

    @Test
    void should_return_unprocessable_message_when_case_id_is_null() throws Exception {
        String messageId = randomMessageId();
        String unprocessableMessage = getCaseEventMessage(null, LocalDateTime.now());


        MvcResult result = mockMvc.perform(post(MESSAGE_ENDPOINT + messageId + "?from_dlq=true")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(unprocessableMessage))
            .andExpect(status().isCreated())
            .andReturn();


        String content = result.getResponse().getContentAsString();
        assertEquals(201, result.getResponse().getStatus(), content);
        assertNotNull(content, MESSAGE);

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        JobResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                        new TypeReference<>() {});
        assertNotNull(mvcResult, MESSAGE);
        assertTrue(response.getMessageIds().contains(messageId));
    }

    @Test
    void should_not_return_an_unprocessable_message_when_case_id_is_valid() throws Exception {
        String messageId = randomMessageId();
        String caseId = randomCaseId();
        String unprocessableMessage = getCaseEventMessage(caseId, LocalDateTime.now());

        MvcResult result = mockMvc.perform(post(MESSAGE_ENDPOINT + messageId + "?from_dlq=true")
                                               .contentType(MediaType.APPLICATION_JSON)
                                               .content(unprocessableMessage))
            .andExpect(status().isCreated())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assertEquals(201, result.getResponse().getStatus(), content);
        assertNotNull(content, MESSAGE);

        MvcResult mvcResult = postAJobRequest(JobName.FIND_PROBLEM_MESSAGES.name());
        JobResponse response = OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                                                        new TypeReference<>() {});
        assertNotNull(mvcResult, MESSAGE);
        assertFalse(response.getMessageIds().contains(messageId));
    }


    private void checkMessagesInState(List<String> messageIds, MessageState messageState) {
        messageIds.forEach(msgId -> {
            try {
                mockMvc.perform(get(MESSAGE_ENDPOINT + msgId))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{'State':'" + messageState.name() + "'}"))
                    .andReturn();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static String getCaseEventMessage(String caseId,LocalDateTime eventTimestamp) {
        return "{\n"
               + "  \"EventInstanceId\" : \"some event instance Id\",\n"
               + "  \"EventTimeStamp\" : \"" + eventTimestamp + "\",\n"
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
    private MvcResult postMessage(String messageId, ResultMatcher created, boolean fromDlq,
                                  LocalDateTime eventTimestamp) throws Exception {
        return mockMvc.perform(post(MESSAGE_ENDPOINT
                                    + messageId + (fromDlq ? "?from_dlq=true" : "?from_dlq=false"))
                                   .contentType(MediaType.APPLICATION_JSON)
                                   .content(getCaseEventMessage(CASE_REFERENCE,eventTimestamp)))
            .andExpect(created)
            .andReturn();
    }

    @NotNull
    private MvcResult postAJobRequest(String jobName) throws Exception {

        return mockMvc.perform(post(ENDPOINT_UNDER_TEST + jobName)
                                   .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
    }
}

