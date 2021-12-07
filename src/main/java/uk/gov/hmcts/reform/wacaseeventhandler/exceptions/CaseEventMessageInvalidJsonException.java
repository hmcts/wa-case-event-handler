package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CaseEventMessageInvalidJsonException extends RuntimeException {

    private static final long serialVersionUID = 7505376830022395265L;

    public CaseEventMessageInvalidJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
