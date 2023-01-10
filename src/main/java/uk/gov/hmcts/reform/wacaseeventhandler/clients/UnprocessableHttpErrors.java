package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.HttpStatus;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public final class UnprocessableHttpErrors {

    private static final List<HttpStatus> UNPROCESSABLE_HTTP_ERRORS =
            List.of(BAD_REQUEST, INTERNAL_SERVER_ERROR);

    private UnprocessableHttpErrors() {
    }

    public static boolean isNonRetryableError(HttpStatus httpStatus) {
        return UNPROCESSABLE_HTTP_ERRORS.contains(httpStatus);
    }
}
