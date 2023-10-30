package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class DelayUntilDateTimeCalculator implements DelayUntilCalculator {

    @Override
    public boolean supports(DelayUntilRequest delayUntilRequest) {
        log.info(
            "Supported DelayUntilConfigurator is {}: ", this.getClass().getName());
        return Optional.ofNullable(delayUntilRequest.getDelayUntilTime()).isPresent()
            && Optional.ofNullable(delayUntilRequest.getDelayUntilOrigin()).isEmpty()
            && Optional.ofNullable(delayUntilRequest.getDelayUntil()).isEmpty();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilRequest delayUntilRequest) {
        log.info("Default Date Time From Instant {} LocalDateTime {}: ", DEFAULT_DATE_TIME, LocalDateTime.now());
        return addTimeToDate(delayUntilRequest.getDelayUntilTime(), DEFAULT_DATE_TIME);
    }

}
