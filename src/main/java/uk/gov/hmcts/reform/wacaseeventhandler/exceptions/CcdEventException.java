package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

public class CcdEventException extends RuntimeException {
    private static final long serialVersionUID = -1303464366505159096L;

    public CcdEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
