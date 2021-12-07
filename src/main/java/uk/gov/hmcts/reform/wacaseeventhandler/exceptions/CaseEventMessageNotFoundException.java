package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CaseEventMessageNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 7388943278923941519L;

    public CaseEventMessageNotFoundException(String message) {
        super(message);
    }
}
