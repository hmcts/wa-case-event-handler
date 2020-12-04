package uk.gov.hmcts.reform.wacaseeventhandler.services;

public interface CaseEventHandler {
    boolean canHandle();

    void handle();
}
