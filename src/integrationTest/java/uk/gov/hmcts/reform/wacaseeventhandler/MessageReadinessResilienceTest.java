package uk.gov.hmcts.reform.wacaseeventhandler;

import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.TransactionTimedOutException;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.config.executors.MessageReadinessExecutor;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.azure.messaging.servicebus.implementation.ManagementConstants.MESSAGE_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=true",
    "azure.servicebus.connection-string="
    + "Endpoint=sb://REPLACE_ME/;SharedAccessKeyName=REPLACE_ME;SharedAccessKey=REPLACE_ME",
    "azure.servicebus.topic-name=test",
    "azure.servicebus.subscription-name=test",
    "azure.servicebus.ccd-case-events-subscription-name=test",
    "azure.servicebus.retry-duration=2",
    "retry.maxAttempts=5",
    "retry.backOff.delay=1000",
    "retry.backOff.maxDelay=3000",
    "retry.backOff.random=true"
})
public class MessageReadinessResilienceTest {

    private static String WARNING_MESSAGE = "An error occurred when running message readiness check. "
                                            + "Catching exception continuing execution";
    private static final int MAX_ATTEMPTS = 5;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Mock
    private TelemetryContext telemetryContext;

    @Mock
    private OperationContext operationContext;

    @MockBean
    private DeadLetterQueuePeekService deadLetterQueuePeekService;

    @MockBean
    private CaseEventMessageRepository caseEventMessageRepository;

    AtomicInteger count;

    @Autowired
    private MessageReadinessExecutor messageReadinessExecutor;

    @BeforeEach
    void setup() {
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
        lenient().when(telemetryContext.getOperation()).thenReturn(operationContext);
        count = new AtomicInteger(0);
    }

    @AfterEach
    void tearDown() {
        messageReadinessExecutor.start();
    }

    @Test
    void should_handle_database_outage_and_log_issue_when_getting_all_messages_in_new_state(CapturedOutput output) {
        doThrow(new JDBCConnectionException("An error occurred when getting all message in new state", null))
            .when(caseEventMessageRepository)
            .getAllMessagesInNewState();

        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                count.set(StringUtils.countMatches(output.getOut(), WARNING_MESSAGE));
                assertEquals(MAX_ATTEMPTS, count.get());
            });
    }

    @Test
    void should_handle_database_outage_and_log_issue_when_updating_message_state(CapturedOutput output) {

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
            .atMost(120, SECONDS)
            .untilAsserted(() -> {
                count.set(StringUtils.countMatches(output.getOut(), WARNING_MESSAGE));
                assertEquals(MAX_ATTEMPTS, count.get());
            });
    }

}
