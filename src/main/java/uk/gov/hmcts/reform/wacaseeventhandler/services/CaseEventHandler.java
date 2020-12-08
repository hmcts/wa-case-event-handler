package uk.gov.hmcts.reform.wacaseeventhandler.services;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.TaskEvaluateDmnResponse;

import java.util.List;


public interface CaseEventHandler {
    List<? extends TaskEvaluateDmnResponse> evaluateDmn(EventInformation eventInformation);

    void handle(List<? extends TaskEvaluateDmnResponse> results, String caseTypeId, String jurisdictionId);
}
