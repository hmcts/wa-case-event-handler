package uk.gov.hmcts.reform.wacaseeventhandler.config.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;

@ControllerAdvice
public class BadRequestAdvice {

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalStateException.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorMessage badRequestHandler(Exception ex) {
        return new ErrorMessage(ex, HttpStatus.BAD_REQUEST, LocalDateTime.now());
    }


}
