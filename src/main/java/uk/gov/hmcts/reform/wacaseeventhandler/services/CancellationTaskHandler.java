package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.cancellationtask.CancellationTaskEvaluateDmnResponse;

import java.util.Collections;
import java.util.List;

@Service
@Order(1)
public class CancellationTaskHandler implements CaseEventHandler {
    @Override
    public List<CancellationTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        // placeholder for the future cancellation process
        return Collections.emptyList();
    }

    @Override
    public void handle(List<? extends TaskEvaluateDmnResponse> results, String caseTypeId, String jurisdictionId) {
        // empty for now
    }

}
