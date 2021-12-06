package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class CaseEventMessageNotFoundException extends RuntimeException {
    public CaseEventMessageNotFoundException(String message) {
        super(message);
    }
}
