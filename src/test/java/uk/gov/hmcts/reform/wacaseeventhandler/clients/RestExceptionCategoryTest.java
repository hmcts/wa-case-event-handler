package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RestExceptionCategoryTest {

    @Test
    void should_return_false_for_a_non_retryable_error() {
        assertFalse(RestExceptionCategory.isRetryableError(HttpStatus.I_AM_A_TEAPOT));
    }

    @Test
    void should_return_true_for_a_4XX_retryable_error() {
        assertTrue(RestExceptionCategory.isRetryableError(HttpStatus.REQUEST_TIMEOUT));
    }

    @Test
    void should_return_true_for_a_401_retryable_error() {
        assertTrue(RestExceptionCategory.isRetryableError(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void should_return_true_for_a_5XX_retryable_error() {
        assertTrue(RestExceptionCategory.isRetryableError(HttpStatus.GATEWAY_TIMEOUT));
    }
}
