package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.HttpStatus;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;


public final class UnprocessableHttpErrors {

    private static final List<HttpStatus> UNPROCESSABLE_HTTP_ERRORS =
            List.of(BAD_REQUEST, NOT_FOUND, FORBIDDEN);

    private UnprocessableHttpErrors() {
    }

    public static boolean isNonRetryableError(HttpStatus httpStatus) {
        return UNPROCESSABLE_HTTP_ERRORS.contains(httpStatus);
    }
}
