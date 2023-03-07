package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class DelayUntilDateCalculator implements DelayUntilCalculator {

    @Override
    public boolean supports(DelayUntilObject delayUntilObject) {
        return Optional.ofNullable(delayUntilObject.getDelayUntil()).isPresent();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilObject delayUntilObject) {
        var delayUntilResponse = delayUntilObject.getDelayUntil();
        var delayUntilTimeResponse = delayUntilObject.getDelayUntilTime();
        if (Optional.ofNullable(delayUntilTimeResponse).isPresent()) {
            return calculateDueDateFrom(delayUntilResponse, delayUntilTimeResponse);
        } else {
            return calculateDueDateFrom(delayUntilResponse);
        }
    }

    private LocalDateTime calculateDueDateFrom(String delayUntil) {
        LocalDateTime parsedDueDate = parseDateTime(delayUntil);
        if (parsedDueDate.getHour() == 0 && parsedDueDate.getMinute() == 0) {
            return parsedDueDate.withHour(16).withMinute(0);
        } else {
            return parsedDueDate;
        }
    }

    private LocalDateTime calculateDueDateFrom(String delayUntil, String delayUntilTimeResponse) {
        return addTimeToDate(delayUntilTimeResponse, parseDateTime(delayUntil));
    }
}
