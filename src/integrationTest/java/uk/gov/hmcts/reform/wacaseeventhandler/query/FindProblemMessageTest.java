package uk.gov.hmcts.reform.wacaseeventhandler.query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.FindProblemMessageJob;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/problem_messages_data.sql")
public class FindProblemMessageTest {

    private final CaseEventMessageMapper caseEventMessageMapper =  new CaseEventMessageMapper();
    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    private FindProblemMessageJob findProblemMessageJob;

    @BeforeEach
    void setUp() {
        findProblemMessageJob = new FindProblemMessageJob(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          60);
    }

    @Test
    void should_retrieve_an_ready_message() {
        List<String> caseEventMessages = findProblemMessageJob.run();
        assertFalse(caseEventMessages.isEmpty());
        assertEquals(3, caseEventMessages.size());
        assertEquals("ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10", caseEventMessages.get(0));
        assertEquals("ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-2704", caseEventMessages.get(1));
        assertEquals("ID:ce8467a0-cea9-4a65-99dd-3ae9a94a4453:16:1:1-811", caseEventMessages.get(2));
    }
}

