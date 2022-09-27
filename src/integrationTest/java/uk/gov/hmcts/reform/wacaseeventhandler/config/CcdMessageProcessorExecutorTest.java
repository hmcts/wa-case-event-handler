package uk.gov.hmcts.reform.wacaseeventhandler.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.applicationinsights.extensibility.context.OperationContext;
import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.DatabaseMessageConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles(profiles = {"db", "integration"})
class CcdMessageProcessorExecutorTest {

    private static final String MESSAGE_ID = "message id";

    private static final String CASE_ID = "case id";

    private static final String SELECT_LOG_MESSAGE = "Selecting next message for processing from the database";

    private static final String PROCESS_LOG_MESSAGE = "Processing message with id: "
        + MESSAGE_ID + " and caseId: " + CASE_ID + " from the database";

    private ListAppender<ILoggingEvent> listAppender;

    @MockBean
    private CaseEventMessageRepository caseEventMessageRepository;

    @MockBean
    private LaunchDarklyFeatureFlagProvider featureFlagProvider;

    @Mock
    private TelemetryContext telemetryContext;

    @Mock
    private OperationContext operationContext;

    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(DatabaseMessageConsumer.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        caseEventMessageEntity.setCaseId(CASE_ID);
        when(caseEventMessageRepository.getNextAvailableMessageReadyToProcess()).thenReturn(caseEventMessageEntity);
        when(featureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
        lenient().when(telemetryContext.getOperation()).thenReturn(operationContext);
    }

    @Test
    void test_create_database_message_consumer_triggers_database_message_consumer() {
        await().until(
            () -> getLogMessageOccurrenceCount(SELECT_LOG_MESSAGE) > 1
                  && getLogMessageOccurrenceCount(PROCESS_LOG_MESSAGE) > 1
        );
    }

    @Test
    void test_create_database_message_consumer_triggers_database_message_consumer_repeatedly() {
        await().atMost(11, SECONDS).until(
            () -> getLogMessageOccurrenceCount(SELECT_LOG_MESSAGE) >= 3L
                  && getLogMessageOccurrenceCount(PROCESS_LOG_MESSAGE) >= 3L
        );
    }

    private long getLogMessageOccurrenceCount(String expectedMessage) {
        List<ILoggingEvent> logsList = List.copyOf(listAppender.list);
        return logsList.stream().filter(x -> x.getFormattedMessage().equals(expectedMessage)).count();
    }

}
