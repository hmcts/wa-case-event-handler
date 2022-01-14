package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestParametersException extends RuntimeException {

    private static final long serialVersionUID = 516726288835875096L;

    public InvalidRequestParametersException(String message) {
        super(message);
    }
}
