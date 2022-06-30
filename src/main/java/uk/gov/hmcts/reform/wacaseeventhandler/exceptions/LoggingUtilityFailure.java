package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

public class LoggingUtilityFailure extends RuntimeException {

    private static final long serialVersionUID = -6206663926409296465L;

    public LoggingUtilityFailure(String message, Throwable cause) {
        super(message, cause);
    }
}
