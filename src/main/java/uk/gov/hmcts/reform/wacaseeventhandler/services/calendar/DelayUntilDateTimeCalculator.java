package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class DelayUntilDateTimeCalculator implements DelayUntilCalculator {

    @Override
    public boolean supports(DelayUntilObject delayUntilObject) {

        return Optional.ofNullable(delayUntilObject.getDelayUntilTime()).isPresent()
            && Optional.ofNullable(delayUntilObject.getDelayUntilOrigin()).isEmpty()
            && Optional.ofNullable(delayUntilObject.getDelayUntil()).isEmpty();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilObject delayUntilObject) {
        return addTimeToDate(delayUntilObject.getDelayUntilTime(), DEFAULT_DATE_TIME);
    }

}
