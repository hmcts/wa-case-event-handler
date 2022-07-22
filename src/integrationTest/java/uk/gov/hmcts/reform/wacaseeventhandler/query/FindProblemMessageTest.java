package uk.gov.hmcts.reform.wacaseeventhandler.query;

import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(caseEventMessages.isEmpty()).isFalse();
        Assertions.assertThat(caseEventMessages.size()).isEqualTo(3);
        Assertions.assertThat(caseEventMessages.get(0)).isEqualTo("8d6cc5cf-c973-11eb-bdba-0242ac111000");
        Assertions.assertThat(caseEventMessages.get(1)).isEqualTo("8d6cc5cf-c973-11eb-bdba-0242ac111001");
        Assertions.assertThat(caseEventMessages.get(2)).isEqualTo("8d6cc5cf-c973-11eb-bdba-0242ac111002");
    }
}

