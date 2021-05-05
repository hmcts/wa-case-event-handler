package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public interface DateFormatter {

    ZonedDateTime formatToZone(LocalDateTime dateTime);
}
