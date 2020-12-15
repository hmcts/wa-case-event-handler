package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.warningtask.WarningTaskEvaluateDmnResponse;

import java.util.Collections;
import java.util.List;

@Service
@Order(2)
public class WarningTaskHandler implements CaseEventHandler {
    @Override
    public List<WarningTaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation) {
        // placeholder for the future cancellation process
        return Collections.emptyList();
    }

    @Override
    public void handle(List<? extends TaskEvaluateDmnResponse> results, EventInformation eventInformation) {
        // empty for now
    }
}
