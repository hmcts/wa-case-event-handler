package uk.gov.hmcts.reform.wacaseeventhandler.services;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.TaskEvaluateDmnResponse;

import java.util.List;


public interface CaseEventHandler {
    List<? extends TaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation);

    void handle(List<? extends TaskEvaluateDmnResponse> results, EventInformation eventInformation);
}
