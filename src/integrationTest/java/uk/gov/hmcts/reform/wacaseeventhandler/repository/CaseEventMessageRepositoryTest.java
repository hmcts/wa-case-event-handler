package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("db")
class CaseEventMessageRepositoryTest {

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_dlq_only.sql"})
    @Test
    void should_not_return_dlq_message_when_no_newer_non_dlq_message_exists() {
        CaseEventMessageEntity message = caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNull(message);
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_dlq_with_newer_non_dlq.sql"})
    @Test
    void should_return_dlq_message_when_newer_non_dlq_ready_exists_for_same_case() {
        CaseEventMessageEntity message = caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNotNull(message);
        assertEquals("MessageId_dlq_1", message.getMessageId());
    }

    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql",
            "classpath:sql/insert_case_event_messages_hold_until_future_and_ready.sql"})
    @Test
    void should_skip_messages_with_future_hold_until() {
        CaseEventMessageEntity message = caseEventMessageRepository.getNextAvailableMessageReadyToProcess();

        assertNotNull(message);
        assertEquals("MessageId_ready", message.getMessageId());
    }
}
