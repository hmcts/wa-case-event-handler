package uk.gov.hmcts.reform.wacaseeventhandler;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.wacaseeventhandler.config.executors.CcdMessageProcessorExecutor;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("db")
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=true",
    "azure.servicebus.connection-string="
    + "Endpoint=sb://REPLACE_ME/;SharedAccessKeyName=REPLACE_ME;SharedAccessKey=REPLACE_ME",
    "azure.servicebus.topic-name=test",
    "azure.servicebus.ccd-case-events-subscription-name=test",
    "azure.servicebus.retry-duration=5",
    "retry.maxAttempts=5",
    "retry.backOff.delay=1000",
    "retry.backOff.maxDelay=3000",
    "retry.backOff.random=true"
})
public class DatabaseMessageConsumerResilienceTest {

    private static final String WARNING_MESSAGE = "An error occurred when running database message consumer. "
                                                  + "Catching exception continuing execution";
    private static final int MAX_ATTEMPTS = 5;

    @MockBean
    private DeadLetterQueuePeekService deadLetterQueuePeekService;
    @MockBean
    CaseEventMessageRepository caseEventMessageRepository;
    @Mock
    private PlatformTransactionManager platformTransactionManager;
    @Mock
    private TransactionTemplate transactionTemplate;

    @Autowired
    private CcdMessageProcessorExecutor ccdMessageProcessorExecutor;

    AtomicInteger count;

    @BeforeEach
    void setup() {
        transactionTemplate.setTransactionManager(platformTransactionManager);
        count = new AtomicInteger(0);
    }

    @AfterEach
    void tearDown() {
        ccdMessageProcessorExecutor.start();
    }

    @Test
    void should_handle_database_outage_and_log_issue_when_database_message_consumer_running(CapturedOutput output) {

        doThrow(new JDBCConnectionException("An error occurred when running database message consumer.", null))
            .when(caseEventMessageRepository)
            .getNextAvailableMessageReadyToProcess();

        await().ignoreException(Exception.class)
            .pollInterval(5, SECONDS)
            .atMost(60, SECONDS)
            .untilAsserted(() -> {
                count.set(StringUtils.countMatches(output.getOut(), WARNING_MESSAGE));
                assertTrue(count.get() > MAX_ATTEMPTS);
            });
    }

}
