package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class  IsoDateFormatter implements DateFormatter {

    @Override
    public String format(LocalDateTime dateTime) {
        ZoneId ukTime = ZoneId.of("Europe/London");
        return dateTime.atZone(ukTime).format(DateTimeFormatter.ISO_INSTANT);
    }
}
