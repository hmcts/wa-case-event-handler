package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_DATE_TIME;

@Slf4j
@Component
public class DelayUntilConfigurator {
    private final List<DelayUntilCalculator> delayUntilCalculators;

    public DelayUntilConfigurator(List<DelayUntilCalculator> delayUntilCalculators) {
        this.delayUntilCalculators = delayUntilCalculators;
    }

    public LocalDateTime calculateDelayUntil(DelayUntilRequest delayUntilRequest) {
        logInput(delayUntilRequest);
        return delayUntilCalculators.stream()
            .filter(delayUntilCalculator -> delayUntilCalculator.supports(delayUntilRequest))
            .findFirst()
            .map(dateCalculator -> dateCalculator.calculateDate(delayUntilRequest))
            .orElse(DEFAULT_DATE_TIME);
    }

    private static void logInput(DelayUntilRequest delayUntilRequest) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            log.info(
                "Delay until value for calculation is : {}",
                objectMapper.writeValueAsString(delayUntilRequest)
            );
        } catch (JsonProcessingException jpe) {
            log.error(jpe.getMessage());
        }

    }
}
