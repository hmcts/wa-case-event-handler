package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wacaseeventhandler.config.job.CleanUpJobConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName.CLEAN_UP_MESSAGES;

@Slf4j
@Service
@Transactional
@SuppressWarnings("PMD.TooManyMethods")
public class CleanUpMessageJob implements MessageJob {

    private final CaseEventMessageRepository caseEventMessageRepository;
    private final CleanUpJobConfiguration cleanUpJobConfiguration;

    public CleanUpMessageJob(CaseEventMessageRepository caseEventMessageRepository,
                             CleanUpJobConfiguration cleanUpJobConfiguration) {
        this.caseEventMessageRepository = caseEventMessageRepository;
        this.cleanUpJobConfiguration = cleanUpJobConfiguration;
    }

    @Override
    public boolean canRun(JobName jobName) {
        return CLEAN_UP_MESSAGES.equals(jobName);
    }

    @Override
    public List<String> run() {

        LocalDateTime deleteBefore = LocalDateTime.now().minusDays(cleanUpJobConfiguration.getStartedDaysBefore());
        log.info("Clean up problem messages for job name '{}' from case db. {} ",
            CLEAN_UP_MESSAGES.name(), cleanUpJobConfiguration);

        if ("prod".equalsIgnoreCase(cleanUpJobConfiguration.getEnvironment())) {

            caseEventMessageRepository.removeOldMessages(
                cleanUpJobConfiguration.getDeleteLimit(),
                cleanUpJobConfiguration.getStateForProd(),
                deleteBefore
            );
        } else {

            caseEventMessageRepository.removeOldMessages(
                cleanUpJobConfiguration.getDeleteLimit(),
                cleanUpJobConfiguration.getStateForNonProd(),
                deleteBefore
            );
        }


        log.info("{} job completed", CLEAN_UP_MESSAGES.name());
        return emptyList();
    }
}
