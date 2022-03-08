package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.model.CaseEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;
import uk.gov.hmcts.reform.wacaseeventhandler.util.LoggingUtility;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.TooManyMethods")
public class ProblemMessageService {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final CaseEventMessageMapper caseEventMessageMapper;
    private final Integer messageTimeLimit;

    public ProblemMessageService(CaseEventMessageRepository caseEventMessageRepository,
                                 CaseEventMessageMapper caseEventMessageMapper,
                                 @Value("${job.problem-message.message-time-limit}")
                                 int messageTimeLimit) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.caseEventMessageMapper = caseEventMessageMapper;
        this.messageTimeLimit = messageTimeLimit;
    }

    public List<CaseEventMessage> findProblemMessages(JobName jobName) {
        log.info("Retrieving problem messages for job name '{}' from case db", jobName.name());
        List<CaseEventMessageEntity> problemMessages = caseEventMessageRepository.findProblemMessages(messageTimeLimit);
        log.info("Retrieved problem message '{}' from case db", problemMessages);
        List<CaseEventMessage> caseEventMessages = problemMessages.stream()
            .map(caseEventMessageEntity -> caseEventMessageMapper.mapToCaseEventMessage(caseEventMessageEntity))
            .collect(Collectors.toList());
        LoggingUtility.logPrettyPrint(caseEventMessages);
        return caseEventMessages;
    }
}
