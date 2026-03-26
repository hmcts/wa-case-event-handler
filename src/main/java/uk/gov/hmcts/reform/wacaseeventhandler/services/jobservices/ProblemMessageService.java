package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ProblemMessageService {
    private final List<MessageJob> messageJobs;

    public ProblemMessageService(List<MessageJob> messageJobs) {
        this.messageJobs = messageJobs;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public List<String> process(JobName jobName) {
        List<String> messages = new ArrayList<>();
        messageJobs.forEach(job -> {
            if (job.canRun(jobName)) {
                log.info("Running job '{}'", jobName.name());
                messages.addAll(job.run());
            }
        });

        return messages;
    }
}
