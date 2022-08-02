package uk.gov.hmcts.reform.wacaseeventhandler.services.jobservices;


import uk.gov.hmcts.reform.wacaseeventhandler.domain.jobs.JobName;

import java.util.List;

public interface MessageJob {

    boolean canRun(JobName jobName);

    List<String> run();
}
