package uk.gov.hmcts.reform.wacaseeventhandler.config.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.UnProcessableEntityException;

import java.time.LocalDateTime;

@ControllerAdvice
public class UnProcessableEntityAdvice {

    @ExceptionHandler(UnProcessableEntityException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorMessage unProcessableEntityHandler(UnProcessableEntityException ex) {
        return new ErrorMessage(ex, HttpStatus.UNPROCESSABLE_ENTITY, LocalDateTime.now());
    }


}
