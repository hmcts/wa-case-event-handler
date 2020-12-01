package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

public class UnProcessableEntityException extends RuntimeException {

    private static final long serialVersionUID = -3771141817920095081L;

    public UnProcessableEntityException(String message) {
        super(message);
    }
}
