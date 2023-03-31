package uk.gov.hmcts.reform.wacaseeventhandler.query;

import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;


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

    private List<String> caseEventMessages;

    @BeforeEach
    void setUp() {
        findProblemMessageJob = new FindProblemMessageJob(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          60);
    }

    @AfterEach
    void tearDown() {
        caseEventMessages = new ArrayList<>();
    }

    @Test
    void should_retrieve_an_ready_message() {
        caseEventMessages = findProblemMessageJob.run();
        Assertions.assertThat(caseEventMessages.isEmpty()).isFalse();
        Assertions.assertThat(caseEventMessages.size()).isEqualTo(3);
        Assertions.assertThat(caseEventMessages.get(0)).isEqualTo("ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10");
        MatcherAssert.assertThat(caseEventMessages, containsInAnyOrder(
            "ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10",
            "ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-2704",
            "ID:ce8467a0-cea9-4a65-99dd-3ae9a94a4453:16:1:1-811"));
    }
}

