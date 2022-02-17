package uk.gov.hmcts.reform.wacaseeventhandler;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
class MessageProcessorTest {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected DataSource db;

    @SpyBean
    private CcdEventProcessor ccdEventProcessor;

    private ListAppender<ILoggingEvent> listAppender;

    private static final String STATE_TEMPLATE = "states=%s";

    private static final String READY_STATE_QUERY = format(STATE_TEMPLATE, MessageState.READY.name());
    private static final String UNPROCESSABLE_STATE_QUERY = format(STATE_TEMPLATE, MessageState.UNPROCESSABLE.name());
    private static final String PROCESSED_STATE_QUERY = format(STATE_TEMPLATE, MessageState.PROCESSED.name());
    private static final String MESSAGE_ID = "MessageId_30915063-ec4b-4272-933d-91087b486195";

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(DatabaseMessageConsumer.class);

        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true).thenReturn(true);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_no_ready_msgs.sql"})
    @Test
    void should_not_process_messages_if_launch_darkly_feature_flag_disabled() throws JsonProcessingException {
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(false);

        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> assertLogMessageContains("No message returned from database for processing"));

        verify(ccdEventProcessor, never()).processMessage(any(CaseEventMessage.class));
        assertTrue(getMessagesInDbFromQuery(READY_STATE_QUERY).isEmpty());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql"})
    @Test
    void should_not_process_messages_if_database_empty() throws JsonProcessingException {
        await()
                .atMost(20, SECONDS)
                .untilAsserted(
                    () -> assertLogMessageContains("Selecting next message for processing from the database")
            );

        verify(ccdEventProcessor, never()).processMessage(any(CaseEventMessage.class));
        assertTrue(getMessagesInDbFromQuery(READY_STATE_QUERY).isEmpty());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_no_ready_msgs.sql"})
    @Test
    void should_not_process_messages_if_no_messages_in_ready_state_exist_in_database() throws JsonProcessingException {
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() ->
                    assertLogMessageContains("Selecting next message for processing from the database")
            );

        verify(ccdEventProcessor, never()).processMessage(any(CaseEventMessage.class));
        assertTrue(getMessagesInDbFromQuery(READY_STATE_QUERY).isEmpty());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_update_hold_until_and_retry_count_for_ready_messages_when_retryable_exception_occurs() {
        String caseId = "6761065058131570";

        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                        assertEquals(2, getMessagesInDbFromQuery(format("case_id=%s", caseId)).size());
                        assertEquals(1, getMessageById(MESSAGE_ID).getRetryCount());
                        assertNotNull(getMessageById(MESSAGE_ID).getHoldUntil());
                }
            );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_set_message_state_to_unprocessable_when_non_retryable_error_occurs() throws JsonProcessingException {
        doThrow(FeignException.BadGateway.class).when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                            assertLogMessageContains(format("Processing message with id %s from the database",
                                    MESSAGE_ID));
                            assertEquals(1, getMessagesInDbFromQuery(UNPROCESSABLE_STATE_QUERY).size());
                        }
            );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_set_message_state_to_unprocessable_when_exception_occurs() throws JsonProcessingException {
        doThrow(IllegalArgumentException.class).when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                            assertLogMessageContains(format("Processing message with id %s from the database",
                                    MESSAGE_ID));
                            assertEquals(1, getMessagesInDbFromQuery(UNPROCESSABLE_STATE_QUERY).size());
                        }
            );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_set_message_state_to_processed_when_message_processed_succesfully() throws JsonProcessingException {
        doNothing().when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                            assertLogMessageContains(format("Processing message with id %s from the database",
                                    MESSAGE_ID));
                            assertEquals(1, getMessagesInDbFromQuery(PROCESSED_STATE_QUERY).size());
                        }
            );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/delete_from_case_event_messages.sql",
                    "classpath:sql/insert_case_event_messages_for_processing_from_dlq.sql"})
    @Test
    void should_set_message_state_to_processed_when_dlq_message_processed_succesfully() throws JsonProcessingException {
        doNothing().when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        await()
                .atMost(20, SECONDS)
                .untilAsserted(() -> {
                            assertLogMessageContains(format("Processing message with id %s from the database",
                                    MESSAGE_ID));
                            assertEquals(1, getMessagesInDbFromQuery(PROCESSED_STATE_QUERY).size());
                        }
            );
    }

    private void assertLogMessageContains(String expectedMessage) {
        List<ILoggingEvent> logsList = listAppender.list;

        assertTrue(logsList.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList())
                .contains(expectedMessage));
    }

    private List<CaseEventMessage> getMessagesInDbFromQuery(String queryString) {
        try {
            final MvcResult mvcResult = mockMvc.perform(get("/messages/query?" + queryString))
                    .andExpect(status().isOk())
                    .andReturn();


            final EventMessageQueryResponse eventMessageQueryResponse =
                    OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                            EventMessageQueryResponse.class);

            return eventMessageQueryResponse.getCaseEventMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    private CaseEventMessage getMessageById(String msgId) throws Exception {
        final MvcResult mvcResult = mockMvc.perform(get(format("/messages/%s", msgId)))
                .andExpect(status().isOk())
                .andReturn();


        return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(),
                CaseEventMessage.class);
    }
}
