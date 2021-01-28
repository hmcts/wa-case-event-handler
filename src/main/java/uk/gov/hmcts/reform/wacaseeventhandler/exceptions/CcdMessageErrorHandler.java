package uk.gov.hmcts.reform.wacaseeventhandler.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.ErrorHandler;

@Slf4j
public class CcdMessageErrorHandler implements ErrorHandler {

    @Override
    public void handleError(@NonNull Throwable throwable) {
        log.error(throwable.getCause().getMessage(), throwable);
    }
}
