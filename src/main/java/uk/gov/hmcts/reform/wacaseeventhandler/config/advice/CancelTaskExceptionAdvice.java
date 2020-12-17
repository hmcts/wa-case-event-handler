package uk.gov.hmcts.reform.wacaseeventhandler.config.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CancelTaskException;

import java.time.LocalDateTime;

@ControllerAdvice
public class CancelTaskExceptionAdvice {

    @ExceptionHandler(CancelTaskException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorMessage cancelTaskExceptionHandler(Exception ex) {
        return new ErrorMessage(ex, HttpStatus.INTERNAL_SERVER_ERROR, LocalDateTime.now());
    }


}
