package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import java.time.LocalDateTime;

public interface DateFormatter {

    String format(LocalDateTime dateTime);
}
