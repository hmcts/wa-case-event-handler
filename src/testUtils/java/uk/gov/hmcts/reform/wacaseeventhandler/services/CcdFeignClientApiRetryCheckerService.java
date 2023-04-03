package uk.gov.hmcts.reform.wacaseeventhandler.services;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class CcdFeignClientApiRetryCheckerService {
    public static final String EXPRESSION = "@ccdFeignClientApiRetryCheckerService.shouldRetry(#root)";

    public boolean shouldRetry(Exception ex) {
        if (ex instanceof FeignException responseException) {
            return HttpStatus.valueOf(responseException.status()).is5xxServerError()
                || HttpStatus.valueOf(responseException.status()).equals(HttpStatus.NOT_FOUND);
        }

        return false;
    }
}
