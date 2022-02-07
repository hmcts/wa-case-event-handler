package uk.gov.hmcts.reform.wacaseeventhandler.services;

public interface DeadLetterQueuePeekService {
    boolean isDeadLetterQueueEmpty();

    void setResponse(boolean response);
}
