package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.LOCKED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_EARLY;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

public final class RestExceptionCategory {

    private static final List<HttpStatus> CLIENT_4XX_RETRYABLE_ERRORS =
            List.of(NOT_FOUND, REQUEST_TIMEOUT, CONFLICT, LOCKED, TOO_EARLY, TOO_MANY_REQUESTS);

    private static final List<HttpStatus> CLIENT_5XX_RETRYABLE_ERRORS =
            List.of(BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT);

    private static final List<HttpStatus> RETRYABLE_ERRORS = Stream.concat(CLIENT_4XX_RETRYABLE_ERRORS.stream(),
                                                                            CLIENT_5XX_RETRYABLE_ERRORS.stream())
                                                                    .collect(Collectors.toList());

    private RestExceptionCategory() {

    }

    public static boolean isRetryableError(HttpStatus httpStatus) {
        return RETRYABLE_ERRORS.contains(httpStatus);
    }
}
