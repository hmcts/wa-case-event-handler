package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

public class UnProcessableEntityException extends RuntimeException {

    public UnProcessableEntityException(String message) {
        super(message);
    }
}
