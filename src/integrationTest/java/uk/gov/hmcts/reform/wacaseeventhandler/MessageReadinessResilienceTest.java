package uk.gov.hmcts.reform.wacaseeventhandler;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionTimedOutException;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.MessageReadinessConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.azure.messaging.servicebus.implementation.ManagementConstants.MESSAGE_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=true",
    "azure.servicebus.connection-string="
    + "Endpoint=sb://REPLACE_ME/;SharedAccessKeyName=REPLACE_ME;SharedAccessKey=REPLACE_ME",
    "azure.servicebus.topic-name=test",
    "azure.servicebus.subscription-name=test",
    "azure.servicebus.ccd-case-events-subscription-name=test",
    "azure.servicebus.retry-duration=2"})
public class MessageReadinessResilienceTest {

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @MockBean
    private TelemetryClient telemetryClient;

    @Mock
    private TelemetryContext telemetryContext;

    @Mock
    private OperationContext operationContext;

    @MockBean
    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @Autowired
    MessageReadinessConsumer messageReadinessConsumer;

    @MockBean
    CaseEventMessageRepository caseEventMessageRepository;

    AtomicInteger count = new AtomicInteger();
    AtomicBoolean isDone = new AtomicBoolean(false);

    @BeforeEach
    void setup() {
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true).thenReturn(true);
        lenient().when(telemetryClient.getContext()).thenReturn(telemetryContext);
        lenient().when(telemetryContext.getOperation()).thenReturn(operationContext);
    }

    @Test
    void should_handle_database_outage_and_log_issue_when_getting_all_messages_in_new_state() {
        doThrow(new JDBCConnectionException("An error occurred when getting all message in new state", null))
            .when(caseEventMessageRepository)
            .getAllMessagesInNewState();

        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                messageReadinessConsumer.run();
                //The purpose of the test: we should see retry when an error occurred.
                //So after 2 trying test will pass
                if (count.get() > 1) {
                    isDone.set(true);
                }
                count.getAndIncrement();
                assertTrue(isDone.get());
            });
    }

    @Test
    void should_handle_database_outage_and_log_issue_when_updating_message_state() {
        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        when(caseEventMessageRepository.getAllMessagesInNewState())
            .thenReturn(List.of(caseEventMessageEntity));

        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty())
            .thenReturn(true);

        doThrow(new TransactionTimedOutException("An error occurred when updating message state"))
            .when(caseEventMessageRepository)
            .updateMessageState(any(), any());


        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                messageReadinessConsumer.run();
                if (count.get() > 1) {
                    isDone.set(true);
                }
                count.getAndIncrement();
                assertTrue(isDone.get());
            });
    }

}
