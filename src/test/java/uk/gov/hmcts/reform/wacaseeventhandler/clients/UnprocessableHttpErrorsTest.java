package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class UnprocessableHttpErrorsTest {

    @Test
    void should_return_true_for_a_non_retryable_error_400_status() {
        assertTrue(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.BAD_REQUEST));
    }

    @Test
    void should_return_false_for_a_non_retryable_error_500_status() {
        assertFalse(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Test
    void should_return_false_for_a_retryable_error() {
        assertFalse(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.I_AM_A_TEAPOT));
    }

    @Test
    void should_return_false_for_a_4XX_retryable_error() {
        assertFalse(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.REQUEST_TIMEOUT));
    }

    @Test
    void should_return_false_for_a_401_non_retryable_error() {
        assertFalse(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void should_return_false_for_a_5XX_retryable_error() {
        assertFalse(UnprocessableHttpErrors.isNonRetryableError(HttpStatus.GATEWAY_TIMEOUT));
    }
}
