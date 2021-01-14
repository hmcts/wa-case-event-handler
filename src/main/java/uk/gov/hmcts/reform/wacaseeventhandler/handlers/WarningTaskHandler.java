package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningEvaluateResponse;

import java.util.Collections;
import java.util.List;

@Service
@Order(2)
public class WarningTaskHandler implements CaseEventHandler {
    @Override
    public List<WarningEvaluateResponse> evaluateDmn(EventInformation eventInformation) {
        // placeholder for the future cancellation process
        return Collections.emptyList();
    }

    @Override
    public void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation) {
        // empty for now
    }
}
