package uk.gov.hmcts.reform.wacaseeventhandler.services;

import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

public interface CaseEventHandler {
    boolean canHandle(EventInformation  eventInformation);

    void handle();
}
