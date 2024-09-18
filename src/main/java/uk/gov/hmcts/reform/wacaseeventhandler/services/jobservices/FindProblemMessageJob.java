package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.ProblemMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventMessageMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.util.LoggingUtility;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName.FIND_PROBLEM_MESSAGES;

@Slf4j
@Service
@Transactional(readOnly = true)
@SuppressWarnings("PMD.TooManyMethods")
public class FindProblemMessageJob implements MessageJob {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final CaseEventMessageMapper caseEventMessageMapper;
    private final Integer messageTimeLimit;

    public FindProblemMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                                 CaseEventMessageMapper caseEventMessageMapper,
                                 @Value("${job.problem-message.message-time-limit}")
                                 int messageTimeLimit) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.caseEventMessageMapper = caseEventMessageMapper;
        this.messageTimeLimit = messageTimeLimit;
    }

    @Override
    public boolean canRun(JobName jobName) {
        return FIND_PROBLEM_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {
        log.info("Retrieving problem messages for job name '{}' from case db", FIND_PROBLEM_MESSAGES.name());
        List<CaseEventMessageEntity> problemMessages = caseEventMessageRepository.findProblemMessages(messageTimeLimit);
        List<ProblemMessage> results = problemMessages.stream()
            .map(caseEventMessageMapper::mapToProblemMessage)
            .collect(Collectors.toList());
        log.info("{} Retrieved problem messages '{}'",
                 FIND_PROBLEM_MESSAGES.name(),
                 results.isEmpty() ?  "no records match the query" : LoggingUtility.logPrettyPrint(results));
        return results.stream().map(ProblemMessage::getMessageId).collect(Collectors.toList());
    }
}
