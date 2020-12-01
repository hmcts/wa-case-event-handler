package uk.gov.hmcts.reform.wacaseeventhandler.services;

public interface CaseEventHandlerService {
    boolean canHandle();

    void handle();
}
