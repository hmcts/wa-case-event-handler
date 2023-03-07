package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DelayUntilConfigurator {
    private final List<DelayUntilCalculator> delayUntilCalculators;

    public DelayUntilConfigurator(List<DelayUntilCalculator> delayUntilCalculators) {
        this.delayUntilCalculators = delayUntilCalculators;
    }

    public LocalDateTime calculateDelayUntil(DelayUntilObject delayUntilObject) {
        return delayUntilCalculators.stream()
            .filter(delayUntilCalculator -> delayUntilCalculator.supports(delayUntilObject))
            .findFirst()
            .map(dateCalculator -> dateCalculator.calculateDate(delayUntilObject))
            .orElse(DelayUntilCalculator.DEFAULT_ZONED_DATE_TIME);
    }
}
