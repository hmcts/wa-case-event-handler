package uk.gov.hmcts.reform.wacaseeventhandler.config.advice;


import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class ErrorMessage {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private final LocalDateTime timestamp;
    private final String error;
    private final int status;
    private final String message;

    public ErrorMessage(Exception ex, HttpStatus error, LocalDateTime timestamp) {
        Objects.requireNonNull(ex, "Exception must not be null");
        Objects.requireNonNull(error, "HttpStatus error must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        this.timestamp = timestamp;
        this.error = error.getReasonPhrase();
        this.status = error.value();
        this.message = ex.getMessage();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getError() {
        return error;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

}
