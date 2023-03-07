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
            return calculateDelayUntilDateFrom(delayUntilResponse, delayUntilTimeResponse);
        } else {
            return calculateDelayUntilDateFrom(delayUntilResponse);
        }
    }

    private LocalDateTime calculateDelayUntilDateFrom(String delayUntil) {
        LocalDateTime parsedDelayUntilDate = parseDateTime(delayUntil);
        if (parsedDelayUntilDate.getHour() == 0 && parsedDelayUntilDate.getMinute() == 0) {
            return parsedDelayUntilDate.withHour(16).withMinute(0);
        } else {
            return parsedDelayUntilDate;
        }
    }

    private LocalDateTime calculateDelayUntilDateFrom(String delayUntil, String delayUntilTimeResponse) {
        return addTimeToDate(delayUntilTimeResponse, parseDateTime(delayUntil));
    }
}
