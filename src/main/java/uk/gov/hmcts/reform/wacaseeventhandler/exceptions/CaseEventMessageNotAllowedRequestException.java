package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception used when we try to access testing endpoints in prod.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CaseEventMessageNotAllowedRequestException extends RuntimeException {

    private static final long serialVersionUID = -1028316931402021753L;
}
