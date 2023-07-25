package uk.gov.hmcts.reform.wacaseeventhandler.query;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices.ProblemMessageService;

import java.util.List;

@SpringBootTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql("/scripts/problem_messages_data.sql")
public class ResetProblemMessageTest {

    @Autowired
    private ProblemMessageService problemMessageService;

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;

    @Test
    void should_retrieve_an_ready_message() {
        List<String> caseEventMessages = problemMessageService.process(JobName.RESET_PROBLEM_MESSAGES);
        Assertions.assertThat(caseEventMessages.isEmpty()).isFalse();
        Assertions.assertThat(caseEventMessages.size()).isEqualTo(2);
        Assertions.assertThat(caseEventMessages.get(0)).isEqualTo("ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-2704");
        Assertions.assertThat(caseEventMessages.get(1)).isEqualTo("ID:ce8467a0-cea9-4a65-99dd-3ae9a94a4453:16:1:1-811");


        List<CaseEventMessageEntity> messages = caseEventMessageRepository.findByMessageId(caseEventMessages);
        messages.forEach(msg -> Assertions.assertThat(msg.getState()).isEqualTo(MessageState.NEW));
    }
}

