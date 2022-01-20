package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("db")
class CaseEventMessageRepositoryTest {

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Autowired
    protected DataSource db;

    private static final String MESSAGE_ID = "MessageId_30915063-ec4b-4272-933d-91087b486195";
    private static final String NON_EXISTENT_MESSAGE_ID = "12345";

    @After
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

    @Test
    void should_return_null_case_event_message_when_table_is_empty() {
        final CaseEventMessageEntity nextAvailableMessageReadyToProcess =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        assertNull(nextAvailableMessageReadyToProcess);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    void should_return_case_event_message_with_message_id() {
        final CaseEventMessageEntity caseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        assertNotNull(caseEventMessageEntity);
        assertEquals("MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a", caseEventMessageEntity.getMessageId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    void should_return_null_case_event_message_when_no_matching_messages_match_query_criteria() {

        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
                "MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a");
        assertEquals(1, caseEventMessageEntities.size());

        final CaseEventMessageEntity caseEventMessageEntity1 = caseEventMessageEntities.get(0);
        assertEquals(MessageState.READY, caseEventMessageEntity1.getState());
        caseEventMessageEntity1.setState(MessageState.PROCESSED);
        caseEventMessageRepository.save(caseEventMessageEntity1);

        final CaseEventMessageEntity caseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        assertNull(caseEventMessageEntity);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should not return any messages when current timestamp is before the hold_until property ")
    void should_return_null_case_event_message_when_no_matching_messages_match_hold_until_query_criteria() {

        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
                "MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a");
        assertEquals(1, caseEventMessageEntities.size());

        final CaseEventMessageEntity caseEventMessageEntity1 = caseEventMessageEntities.get(0);
        assertEquals(MessageState.READY, caseEventMessageEntity1.getState());
        caseEventMessageEntity1.setHoldUntil(LocalDateTime.now().plusDays(2));
        caseEventMessageEntity1.setEventTimestamp(LocalDateTime.now());
        caseEventMessageRepository.save(caseEventMessageEntity1);

        final CaseEventMessageEntity caseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        assertNull(caseEventMessageEntity);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should return message when current timestamp is after the hold_until property")
    void should_return_case_event_message_when_messages_match_hold_until_query_criteria() {

        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
                MESSAGE_ID);
        assertEquals(1, caseEventMessageEntities.size());

        final CaseEventMessageEntity caseEventMessageEntity1 = caseEventMessageEntities.get(0);
        assertEquals(MessageState.NEW, caseEventMessageEntity1.getState());
        caseEventMessageEntity1.setState(MessageState.READY);
        caseEventMessageEntity1.setCaseId("293e1db4-dfd7-433d-902c-39470386a32c");

        final LocalDateTime yearAgoFromCurrentEventTimestamp =
                caseEventMessageEntity1.getEventTimestamp().minusYears(1);
        caseEventMessageEntity1.setHoldUntil(yearAgoFromCurrentEventTimestamp);
        caseEventMessageEntity1.setEventTimestamp(yearAgoFromCurrentEventTimestamp.plusDays(2));
        caseEventMessageRepository.save(caseEventMessageEntity1);

        final CaseEventMessageEntity caseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();
        assertNotNull(caseEventMessageEntity);
        assertEquals(MESSAGE_ID, caseEventMessageEntity.getMessageId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_update_case_event_message_state_when_message_exists() {

        final int rowsAffected = caseEventMessageRepository.updateMessageState(MessageState.PROCESSED, MESSAGE_ID);

        assertEquals(1, rowsAffected);

        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
                MESSAGE_ID);
        assertEquals(1, caseEventMessageEntities.size());
        assertEquals(MessageState.PROCESSED, caseEventMessageEntities.get(0).getState());
    }


    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_not_update_case_event_message_that_does_not_exist() {

        final int rowsAffected =
                caseEventMessageRepository.updateMessageState(MessageState.PROCESSED, NON_EXISTENT_MESSAGE_ID);

        assertEquals(0, rowsAffected);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message retry details")
    @Transactional
    void should_update_case_event_message_retry_details() {

        final LocalDateTime nowPlusTwoHours = LocalDateTime.now();
        final int retryCount = 5;
        final int rowsAffected =
                caseEventMessageRepository.updateMessageWithRetryDetails(retryCount, nowPlusTwoHours, MESSAGE_ID);

        assertEquals(1, rowsAffected);
        final List<CaseEventMessageEntity> caseEventMessageEntities = caseEventMessageRepository.findByMessageId(
                MESSAGE_ID);

        assertEquals(1, caseEventMessageEntities.size());
        assertEquals(retryCount, caseEventMessageEntities.get(0).getRetryCount());
        assertEquals(nowPlusTwoHours, caseEventMessageEntities.get(0).getHoldUntil());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages.sql"})
    @Test
    @DisplayName("Should update specified message state")
    @Transactional
    void should_not_update_case_event_message_with_retry_details_that_does_not_exist() {

        System.out.println("HERE");
        final int rowsAffected = caseEventMessageRepository.updateMessageWithRetryDetails(10,
                LocalDateTime.now().plusHours(2),
                NON_EXISTENT_MESSAGE_ID);

        assertEquals(0, rowsAffected);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages_from_dlq.sql"})
    @Test
    void should_select_dlq_message_where_other_processed_or_ready_messages_exist_with_timestamp_later_than_30mins() {
        final CaseEventMessageEntity retrievedCaseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNotNull(retrievedCaseEventMessageEntity);
        assertEquals("MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a", retrievedCaseEventMessageEntity.getMessageId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages_from_dlq.sql"})
    @Test
    void should_return_null_where_other_processed_or_ready_messages_exist_with_timestamp_earlier_than_30mins() {
        final List<CaseEventMessageEntity> caseEventMessageEntities =
                caseEventMessageRepository.findByMessageId("MessageId_37f7a172-79e6-11ec-90d6-0242ac120003");

        assertEquals(1, caseEventMessageEntities.size());
        final CaseEventMessageEntity caseEventMessageEntity = caseEventMessageEntities.get(0);

        final LocalDateTime eventTimestamp = caseEventMessageEntity.getEventTimestamp();
        caseEventMessageEntity.setEventTimestamp(eventTimestamp.minusHours(3).minusMinutes(40));
        caseEventMessageRepository.save(caseEventMessageEntity);

        final CaseEventMessageEntity retrievedCaseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNull(retrievedCaseEventMessageEntity);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
            scripts = {"classpath:sql/insert_case_event_messages_from_dlq.sql"})
    @Test
    void should_return_null_where_no_messages_from_dlq_with_same_case_id_exist() {
        changeCaseIdAndSetFromDlq("MessageId_6cecf982-6b9e-4cc3-a8f5-d04b1385c258", "unknownCase");
        changeCaseIdAndSetFromDlq("MessageId_37f7a172-79e6-11ec-90d6-0242ac120003", "unknownCase1");
        final CaseEventMessageEntity retrievedCaseEventMessageEntity =
                caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNull(retrievedCaseEventMessageEntity);
    }

    private void changeCaseIdAndSetFromDlq(String caseEventMessageId, String newCaseIdValue) {
        final List<CaseEventMessageEntity> caseEventMessageEntities =
                caseEventMessageRepository.findByMessageId(caseEventMessageId);

        assertEquals(1, caseEventMessageEntities.size());
        final CaseEventMessageEntity caseEventMessageEntity = caseEventMessageEntities.get(0);

        caseEventMessageEntity.setCaseId(newCaseIdValue);
        caseEventMessageEntity.setFromDlq(true);
        caseEventMessageRepository.save(caseEventMessageEntity);
    }
}