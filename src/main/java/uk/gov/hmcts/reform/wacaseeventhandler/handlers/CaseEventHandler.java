package uk.gov.hmcts.reform.wacaseeventhandler.handlers;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;

import java.util.List;


public interface CaseEventHandler {
    List<? extends EvaluateResponse> evaluateDmn(EventInformation eventInformation);

    void handle(List<? extends EvaluateResponse> results, EventInformation eventInformation);
}
