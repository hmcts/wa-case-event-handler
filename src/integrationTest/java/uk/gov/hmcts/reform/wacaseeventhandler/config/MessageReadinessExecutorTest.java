package uk.gov.hmcts.reform.wacaseeventhandler.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.MessageReadinessConsumer;
import uk.gov.hmcts.reform.wacaseeventhandler.config.executors.MessageReadinessExecutor;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.DeadLetterQueuePeekService;

import java.util.List;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageReadinessExecutorTest {

    private static final String MESSAGE_ID = "message id";

    private static final String PROCESS_LOG_MESSAGE = "Updating following message to READY state " + MESSAGE_ID;

    private ListAppender<ILoggingEvent> listAppender;

    @Mock
    private DeadLetterQueuePeekService deadLetterQueuePeekService;
    @Mock
    private CaseEventMessageRepository caseEventMessageRepository;


    @BeforeEach
    void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger(MessageReadinessConsumer.class);

        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(List.of(caseEventMessageEntity));
    }

    @Test
    void should_create_executor_that_repeatedly_calls_message_readiness_consumer() {
        MessageReadinessExecutor messageReadinessExecutor = new MessageReadinessExecutor();
        MessageReadinessConsumer messageReadinessConsumer =
                new MessageReadinessConsumer(deadLetterQueuePeekService, caseEventMessageRepository);
        ReflectionTestUtils.setField(messageReadinessExecutor, "messageReadinessConsumer",
                                     messageReadinessConsumer);
        ReflectionTestUtils.setField(messageReadinessExecutor, "messageReadinessExecutorService",
                                     Executors.newScheduledThreadPool(1));
        ReflectionTestUtils.setField(messageReadinessExecutor, "pollInterval", 2);

        CaseEventMessageEntity caseEventMessageEntity = new CaseEventMessageEntity();
        caseEventMessageEntity.setMessageId(MESSAGE_ID);
        when(caseEventMessageRepository.getAllMessagesInNewState()).thenReturn(List.of(caseEventMessageEntity));
        when(deadLetterQueuePeekService.isDeadLetterQueueEmpty()).thenReturn(true);
        messageReadinessExecutor.start();

        await().until(
            () -> getLogMessageOccurrenceCount(PROCESS_LOG_MESSAGE) > 2
        );

        messageReadinessExecutor.cleanup();
    }

    private long getLogMessageOccurrenceCount(String expectedMessage)  {
        List<ILoggingEvent> logsList = List.copyOf(listAppender.list);
        return logsList.stream().filter(x -> x.getFormattedMessage().startsWith(expectedMessage)).count();
    }
}
