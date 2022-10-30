package uk.gov.hmcts.reform.wacaseeventhandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.TransactionTimedOutException;
import uk.gov.hmcts.reform.wacaseeventhandler.config.executors.CcdMessageProcessorExecutor;
import uk.gov.hmcts.reform.wacaseeventhandler.config.executors.MessageReadinessExecutor;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.EventMessageQueryResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.ccd.CcdEventProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
public class UpdateRecordErrorHandlingTest {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());
    private static final String MESSAGE_ID = "MessageId_30915063-ec4b-4272-933d-91087b486195";
    private static final String MESSAGE_ID_2 = "MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a";

    @Mock
    private TelemetryContext telemetryContext;

    @Mock
    private OperationContext operationContext;

    @SpyBean
    private CcdEventProcessor ccdEventProcessor;

    @SpyBean
    private CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CcdMessageProcessorExecutor ccdMessageProcessorExecutor;

    @Autowired
    private MessageReadinessExecutor messageReadinessExecutor;

    private static final String STATE_TEMPLATE = "states=%s";
    private static final String PROCESSED_STATE_QUERY = format(STATE_TEMPLATE, MessageState.PROCESSED.name());
    private static final String UNPROCESSABLE_STATE_QUERY = format(STATE_TEMPLATE, MessageState.UNPROCESSABLE.name());

    @BeforeEach
    void setup() {
        lenient().when(telemetryContext.getOperation()).thenReturn(operationContext);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_for_processing_from_dlq.sql"})
    @Test
    void should_set_message_state_to_processed_when_message_update_failed_in_first_time()
        throws JsonProcessingException {
        doNothing().when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        doThrow(new TransactionTimedOutException("Time out")).when(caseEventMessageRepository)
            .updateMessageState(eq(MessageState.PROCESSED), Mockito.<String>anyList());
        await()
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertEquals(1, getMessagesInDbFromQuery(PROCESSED_STATE_QUERY).size()));
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_update_hold_until_and_retry_count_for_ready_messages_when_retryable_exception_occurs()
        throws JsonProcessingException {
        String caseId = "6761065058131570";

        MessageProcessorTest.RetryableFeignException retryableFeignException = new MessageProcessorTest
            .RetryableFeignException(504, "Gateway Timeout");
        doThrow(retryableFeignException)
            .when(ccdEventProcessor)
            .processMessage(any(CaseEventMessage.class));
        doThrow(new TransactionTimedOutException("Time out")).when(caseEventMessageRepository)
            .updateMessageWithRetryDetails(eq(1), any(LocalDateTime.class), eq(MESSAGE_ID));

        await()
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                assertEquals(2, getMessagesInDbFromQuery(format("case_id=%s", caseId)).size());
                assertEquals(1, getMessageById(MESSAGE_ID).getRetryCount());
                assertNotNull(getMessageById(MESSAGE_ID).getHoldUntil());
            });
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_set_message_state_to_processed_when_message_update_failed_in_second_time_onwards()
        throws JsonProcessingException {
        messageReadinessExecutor.start();
        ccdMessageProcessorExecutor.start();
        String caseId = "9140931237014412";

        doNothing().when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        doThrow(new TransactionTimedOutException("Time out")).when(caseEventMessageRepository)
            .updateMessageState(eq(MessageState.PROCESSED), Mockito.<String>anyList());

        await()
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                assertEquals(1, getMessagesInDbFromQuery(format("case_id=%s", caseId)).size());
                assertEquals(MessageState.PROCESSED, getMessageById(MESSAGE_ID_2).getState());
            });
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_for_processing_ready_msgs.sql"})
    @Test
    void should_set_message_state_to_unprocessable_when_non_retryable_error_occurs() throws JsonProcessingException {
        doThrow(FeignException.NotFound.class).when(ccdEventProcessor).processMessage(any(CaseEventMessage.class));
        doThrow(new TransactionTimedOutException("Time out")).when(caseEventMessageRepository)
            .updateMessageState(eq(MessageState.UNPROCESSABLE), Mockito.<String>anyList());
        await()
            .atMost(60, SECONDS)
            .untilAsserted(() -> assertEquals(1, getMessagesInDbFromQuery(UNPROCESSABLE_STATE_QUERY).size()));
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
