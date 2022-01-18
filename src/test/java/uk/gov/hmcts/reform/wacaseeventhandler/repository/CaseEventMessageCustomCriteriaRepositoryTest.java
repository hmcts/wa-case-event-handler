package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("db")
class CaseEventMessageCustomCriteriaRepositoryTest {
    @Autowired
    private CaseEventMessageCustomCriteriaRepository repository;

    @Autowired
    protected DataSource db;

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_return_case_event_messages() {
        final Long numberOfMessages = repository.countAll();
        assertEquals(3, numberOfMessages);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_with_all_parameters_and_return_case_event_message() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                List.of(MessageState.NEW, MessageState.READY),
                "8375-3716-6885-2639",
                LocalDateTime.of(2022,1,4,12,42,46,6026000),
                Boolean.TRUE
            );
        assertEquals(1, messageEntities.size());
        assertEquals("8375-3716-6885-2639", messageEntities.get(0).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_multiple_states_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                List.of(MessageState.NEW, MessageState.READY),
                null,
                null,
                null
            );
        assertEquals(3, messageEntities.size());
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("6761-0650-5813-1570"));
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("8375-3716-6885-2639"));
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("9140-9312-3701-4412"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_state_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                List.of(MessageState.NEW),
                null,
                null,
                null
            );
        assertEquals(2, messageEntities.size());
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("6761-0650-5813-1570"));
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("8375-3716-6885-2639"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_caseId_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                null,
                "9140-9312-3701-4412",
                null,
                null
            );
        assertEquals(1, messageEntities.size());
        assertEquals("9140-9312-3701-4412", messageEntities.get(0).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_eventTimestamp_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                null,
                null,
                LocalDateTime.of(2022,1,14,12,45,54, 887100),
                null
            );
        assertEquals(1, messageEntities.size());
        assertEquals("9140-9312-3701-4412", messageEntities.get(0).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_fromDlq_equal_true_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                null,
                null,
                null,
                Boolean.TRUE
            );
        assertEquals(1, messageEntities.size());
        assertEquals("8375-3716-6885-2639", messageEntities.get(0).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/delete_from_case_event_messages.sql", "classpath:sql/insert_case_event_messages.sql"})
    void should_query_by_fromDlq_equal_false_and_return_case_event_messages() {
        final List<CaseEventMessageEntity> messageEntities =
            repository.getMessages(
                null,
                null,
                null,
                Boolean.FALSE
            );
        assertEquals(2, messageEntities.size());
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("6761-0650-5813-1570"));
        assertTrue(messageEntities.stream().map(CaseEventMessageEntity::getCaseId).collect(Collectors.toList())
                       .contains("9140-9312-3701-4412"));
    }
}
