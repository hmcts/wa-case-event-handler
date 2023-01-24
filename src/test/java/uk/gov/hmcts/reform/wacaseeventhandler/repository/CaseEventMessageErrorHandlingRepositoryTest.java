package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.hmcts.reform.wacaseeventhandler.util.TestFixtures.createCaseEventMessageEntity;

@SpringBootTest
@ActiveProfiles("db")
class CaseEventMessageErrorHandlingRepositoryTest {

    @Autowired
    private CaseEventMessageErrorHandlingRepository errorHandlingRepository;

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    protected DataSource db;

    @Autowired
    PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    private static final String MESSAGE_ID = "MessageId_30915063-ec4b-4272-933d-91087b486195";
    private static final String NON_EXISTENT_MESSAGE_ID = "12345";

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    public void clearDownData() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);

        String truncateTablesQuery =
                "START TRANSACTION;\n"
                + "TRUNCATE TABLE WA_CASE_EVENT_MESSAGES CASCADE;"
                + "\nCOMMIT;";
        jdbcTemplate.execute(truncateTablesQuery);

        jdbcTemplate.execute("ALTER SEQUENCE WA_CASE_EVENT_MESSAGES_SEQUENCE_SEQ RESTART WITH 1");
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_update_case_event_message_state_when_message_exists() {

        final int rowsAffected =
            errorHandlingRepository.updateMessageState(MessageState.PROCESSED, List.of(MESSAGE_ID));

        assertEquals(1, rowsAffected);

        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
            singletonList(MESSAGE_ID));
        assertEquals(1, caseEventMessageEntities.size());
        assertEquals(MessageState.PROCESSED, caseEventMessageEntities.get(0).getState());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    void should_update_multiple_case_event_message_states() {

        final CaseEventMessageEntity caseEventMessageEntity = createCaseEventMessageEntity();
        caseEventMessageRepository.save(caseEventMessageEntity);

        final List<CaseEventMessageEntity> allMessagesInNewState =
                caseEventMessageRepository.getAllMessagesInNewState();

        List<String> messageIds = allMessagesInNewState.stream()
                .map(CaseEventMessageEntity::getMessageId)
                .collect(Collectors.toList());

        transactionTemplate.execute(status -> errorHandlingRepository.updateMessageState(MessageState.PROCESSED,
                messageIds));

        messageIds.forEach(msgId -> assertMessageState(msgId, MessageState.PROCESSED));
    }

    private void assertMessageState(String messageId, MessageState messageState) {
        caseEventMessageRepository.findByMessageId(singletonList(messageId))
            .stream()
            .findFirst()
            .ifPresentOrElse(
                msg -> assertEquals(messageState, msg.getState()),
                () -> fail("Did not receive message with id " + messageId)
            );
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_not_update_case_event_message_that_does_not_exist() {

        final int rowsAffected =
            errorHandlingRepository.updateMessageState(MessageState.PROCESSED, List.of(NON_EXISTENT_MESSAGE_ID));

        assertEquals(0, rowsAffected);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message retry details")
    @Transactional
    void should_update_case_event_message_retry_details() {

        final LocalDateTime nowPlusTwoHours = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        final int retryCount = 5;
        final int rowsAffected =
            errorHandlingRepository.updateMessageWithRetryDetails(retryCount, nowPlusTwoHours, MESSAGE_ID);

        assertEquals(1, rowsAffected);
        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
            singletonList(MESSAGE_ID));

        assertEquals(1, caseEventMessageEntities.size());
        assertEquals(retryCount, caseEventMessageEntities.get(0).getRetryCount());
        assertEquals(nowPlusTwoHours, caseEventMessageEntities.get(0).getHoldUntil().truncatedTo(ChronoUnit.SECONDS));
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_not_update_case_event_message_with_retry_details_that_does_not_exist() {

        final int rowsAffected = errorHandlingRepository.updateMessageWithRetryDetails(10,
                LocalDateTime.now().plusHours(2),
                NON_EXISTENT_MESSAGE_ID);

        assertEquals(0, rowsAffected);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @Transactional
    void should_update_message_state_when_find_by_id_to_update() {
        List<CaseEventMessageEntity> caseEventMessageEntity =
            errorHandlingRepository.findByMessageIdToUpdate("MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a");
        assertNotNull(caseEventMessageEntity);
        assertEquals(1, caseEventMessageEntity.size());
        assertEquals("MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a", caseEventMessageEntity.get(0).getMessageId());

        AtomicInteger rowsAffected = new AtomicInteger();
        transactionTemplate.execute(status -> {
            rowsAffected.set(errorHandlingRepository
                                 .updateMessageState(MessageState.PROCESSED,
                                                    List.of("MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a")
            ));
            return true;
        });
        assertEquals(1, rowsAffected.get());

    }
}
