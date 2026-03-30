package uk.gov.hmcts.reform.wacaseeventhandler.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.FindProblemMessageJob;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;


@ActiveProfiles("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Sql("/scripts/problem_messages_data.sql")
public class FindProblemMessageTest {


    private final ObjectMapper objectMapper =  new ObjectMapper();
    private final CaseEventMessageMapper caseEventMessageMapper = new CaseEventMessageMapper(objectMapper);

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Value("${job.problem-message.message-time-limit}")
    private int messageTimeLimit;

    private FindProblemMessageJob findProblemMessageJob;

    private List<String> caseEventMessages;

    @BeforeEach
    void setUp() {
        findProblemMessageJob = new FindProblemMessageJob(caseEventMessageRepository,
                                                          caseEventMessageMapper,
                                                          messageTimeLimit);
    }

    @AfterEach
    void tearDown() {
        caseEventMessages = new ArrayList<>();
    }

    @Test
    void should_retrieve_an_ready_message() {
        caseEventMessages = findProblemMessageJob.run();
        Assertions.assertThat(caseEventMessages.isEmpty()).isFalse();
        Assertions.assertThat(caseEventMessages.size()).isEqualTo(4);
        Assertions.assertThat(caseEventMessages.get(0)).isEqualTo("ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10");
        MatcherAssert.assertThat(caseEventMessages, containsInAnyOrder(
            "ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10",
            "ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-2704",
            "ID:ce8467a0-cea9-4a65-99dd-3ae9a94a4453:16:1:1-811",
            "ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:2-1675"));
    }

    @Test
    void should_only_return_ready_messages_older_than_the_limit() {
        caseEventMessageRepository.deleteAll();
        caseEventMessageRepository.saveAll(List.of(
            createMessage("ready-older-than-limit", "case-1", MessageState.READY, messageTimeLimit + 1),
            createMessage("ready-at-limit", "case-2", MessageState.READY, messageTimeLimit),
            createMessage("ready-newer-than-limit", "case-3", MessageState.READY, messageTimeLimit - 1),
            createMessage("unprocessable-message", "case-4", MessageState.UNPROCESSABLE, 5)
        ));

        caseEventMessages = findProblemMessageJob.run();

        Assertions.assertThat(caseEventMessages)
            .doesNotContain("ready-at-limit", "ready-newer-than-limit");
        MatcherAssert.assertThat(caseEventMessages, containsInAnyOrder(
            "ready-older-than-limit",
            "unprocessable-message"));
    }

    private CaseEventMessageEntity createMessage(String messageId,
                                                 String caseId,
                                                 MessageState state,
                                                 long ageInMinutes) {
        LocalDateTime timestamp = LocalDateTime.now().minusMinutes(ageInMinutes);

        CaseEventMessageEntity message = new CaseEventMessageEntity();
        message.setMessageId(messageId);
        message.setCaseId(caseId);
        message.setEventTimestamp(timestamp);
        message.setFromDlq(false);
        message.setState(state);
        message.setMessageProperties(objectMapper.createObjectNode());
        message.setMessageContent("{\"CaseTypeId\":\"WaCaseType\"}");
        message.setReceived(timestamp);
        message.setDeliveryCount(1);
        message.setHoldUntil(timestamp);
        message.setRetryCount(0);
        return message;
    }
}
