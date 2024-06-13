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

        return Optional.ofNullable(delayUntilRequest.getDelayUntilTime()).isPresent()
            && Optional.ofNullable(delayUntilRequest.getDelayUntilOrigin()).isEmpty()
            && Optional.ofNullable(delayUntilRequest.getDelayUntil()).isEmpty();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilRequest delayUntilRequest) {
        return addTimeToDate(delayUntilRequest.getDelayUntilTime(), LocalDateTime.now());
    }

}
