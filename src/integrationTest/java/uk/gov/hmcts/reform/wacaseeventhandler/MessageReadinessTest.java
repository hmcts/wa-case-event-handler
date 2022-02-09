package uk.gov.hmcts.reform.wacaseeventhandler;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
@TestPropertySource(properties = {"azure.servicebus.enableASB=true"})
public class MessageReadinessTest {

    private static final boolean DLQ_EMPTY = true;
    private static final boolean DLQ_NOT_EMPTY = false;

    @MockBean
    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Autowired
    private MockMvc mockMvc;

    private String randomMessageId() {
        return "" + ThreadLocalRandom.current().nextLong(1000000);
    }

    private static String getCaseEventMessage() {
        return "{\n"
                + "  \"EventInstanceId\" : \"some event instance Id\",\n"
                + "  \"EventTimeStamp\" : \"" + LocalDateTime.now() + "\",\n"
                + "  \"CaseId\" : \"caseIdValue\",\n"
                + "  \"JurisdictionId\" : \"ia\",\n"
                + "  \"CaseTypeId\" : \"asylum\",\n"
                + "  \"EventId\" : \"some event Id\",\n"
                + "  \"NewStateId\" : \"some new state Id\",\n"
                + "  \"UserId\" : \"process_true\",\n"
                + "  \"MessageProperties\" : {\n"
                + "      \"property1\" : \"test1\"\n"
                + "  }\n"
                + "}";
    }

    private MvcResult postMessage(String messageId) throws Exception {
        return mockMvc.perform(put("/messages/" + messageId + "?from_dlq=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getCaseEventMessage()))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private static Stream<Arguments> provideDlqStateAndExpectedMessageState() {
        return Stream.of(
                Arguments.of(DLQ_EMPTY, MessageState.READY),
                Arguments.of(DLQ_NOT_EMPTY, MessageState.NEW)
        );
    }

    @ParameterizedTest
    @MethodSource("provideDlqStateAndExpectedMessageState")
    public void should_check_message_state_according_to_dlq_content(boolean isDlqEmpty, MessageState expectedState)
            throws Exception {
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true).thenReturn(true);

        String messageId = randomMessageId();
        String messageId2 = randomMessageId();

        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(isDlqEmpty);

        postMessage(messageId);
        postMessage(messageId2);

        await().pollInterval(500, MILLISECONDS)
                .atMost(45, SECONDS)
                .untilAsserted(() ->  checkMessagesInState(List.of(messageId, messageId2), expectedState));
    }


    private void checkMessagesInState(List<String> messageIds, MessageState messageState) {
        messageIds.forEach(msgId -> {
            try {
                mockMvc.perform(get("/messages/" + msgId))
                        .andExpect(status().isOk())
                        .andExpect(content().json("{'State':'" + messageState.name() + "'}")).andReturn();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
