package uk.gov.hmcts.reform.wacaseeventhandler.services;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import java.util.List;


public interface CaseEventHandler {
    List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation);

    void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation);
}
