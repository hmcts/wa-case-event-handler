package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.HttpStatus;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public final class RestExceptionCategory {

    private static final List<HttpStatus> NOT_PROCESSABLE_HTTP_ERRORS =
            List.of(BAD_REQUEST, INTERNAL_SERVER_ERROR);

    private RestExceptionCategory() {
    }

    public static boolean isRetryableError(HttpStatus httpStatus) {
        return !NOT_PROCESSABLE_HTTP_ERRORS.contains(httpStatus);
    }
}
