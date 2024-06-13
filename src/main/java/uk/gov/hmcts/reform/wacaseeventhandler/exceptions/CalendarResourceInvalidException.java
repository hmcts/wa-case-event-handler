package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

public class CalendarResourceInvalidException extends RuntimeException {
    private static final long serialVersionUID = -3610389879304395229L;

    public CalendarResourceInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
