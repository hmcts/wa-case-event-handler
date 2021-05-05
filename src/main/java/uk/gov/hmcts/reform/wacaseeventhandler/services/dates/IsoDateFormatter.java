package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class IsoDateFormatter implements DateFormatter {

    @Override
    public ZonedDateTime formatToZone(LocalDateTime dateTime) {
        ZoneId ukTime = ZoneId.of("Europe/London");
        return ZonedDateTime.of(dateTime, ukTime);
    }
}
