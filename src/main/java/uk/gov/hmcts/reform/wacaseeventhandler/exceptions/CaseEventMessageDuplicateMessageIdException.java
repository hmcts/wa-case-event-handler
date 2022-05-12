package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CaseEventMessageDuplicateMessageIdException extends RuntimeException {
    private static final long serialVersionUID = 296433037447741435L;

    public CaseEventMessageDuplicateMessageIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
