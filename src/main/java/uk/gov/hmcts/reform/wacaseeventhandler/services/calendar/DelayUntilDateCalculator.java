package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Slf4j
@Component
public class DelayUntilDateCalculator implements DelayUntilCalculator {

    @Override
    public boolean supports(DelayUntilRequest delayUntilRequest) {
        log.info(
            "Supported DelayUntilConfigurator is {}: " + this.getClass().getName());
        return Optional.ofNullable(delayUntilRequest.getDelayUntil()).isPresent();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilRequest delayUntilRequest) {
        var delayUntilResponse = delayUntilRequest.getDelayUntil();
        var delayUntilTimeResponse = delayUntilRequest.getDelayUntilTime();
        if (Optional.ofNullable(delayUntilTimeResponse).isPresent()) {
            return addTimeToDate(delayUntilTimeResponse, parseDateTime(delayUntilResponse));
        } else {
            LocalDateTime parseDateTime = parseDateTime(delayUntilResponse);
            if (parseDateTime.getHour() == 0 && parseDateTime.getMinute() == 0) {
                LocalTime localTime = LocalTime.now();
                return parseDateTime.withHour(localTime.getHour()).withMinute(localTime.getMinute());
            } else {
                return parseDateTime;
            }
        }
    }

}
