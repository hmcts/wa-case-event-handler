package uk.gov.hmcts.reform.wacaseeventhandler.config.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.UnProcessableEntityException;

@ControllerAdvice
public class UnProcessableEntityAdvice {

    @ResponseBody
    @ExceptionHandler(UnProcessableEntityException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    String unProcessableEntityHandler(UnProcessableEntityException ex) {
        return ex.getMessage();
    }


}
