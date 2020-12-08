package uk.gov.hmcts.reform.wacaseeventhandler.services;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

import java.util.List;


public interface CaseEventHandler<T> {
    List<T> evaluateDmn(EventInformation  eventInformation);

    void handle(List<T> results, String caseTypeId, String jurisdictionId);
}
