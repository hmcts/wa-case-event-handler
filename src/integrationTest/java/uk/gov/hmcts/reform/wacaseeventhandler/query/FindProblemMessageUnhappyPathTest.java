package uk.gov.hmcts.reform.wacaseeventhandler.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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


@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/problem_messages_unhappy_path_data.sql")
public class FindProblemMessageUnhappyPathTest {
    @Mock
    private ObjectMapper objectMapper = new ObjectMapper();

    private final CaseEventMessageMapper caseEventMessageMapper =  new CaseEventMessageMapper(objectMapper);

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    private FindProblemMessageJob problemMessageService;

    @BeforeEach
    void setUp() {
        problemMessageService = new FindProblemMessageJob(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          60);
    }

    @Test
    void should_not_retrieve_any_problem_messages() {
        List<String> caseEventMessages = problemMessageService.run();
        Assertions.assertThat(caseEventMessages.isEmpty()).isTrue();
    }
}

