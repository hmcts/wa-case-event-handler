package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IsoDateFormatterTest {

    @Test
    void format() {
        IsoDateFormatter isoDateFormatter = new IsoDateFormatter();

        String fixedDate = "2020-02-23T12:56:19.403975";

        String expectedDate = "2020-02-23T12:56:19.403975Z[Europe/London]";

        ZonedDateTime zonedDateTime = isoDateFormatter.formatToZone(LocalDateTime.parse(fixedDate));

        assertThat(expectedDate).isEqualTo(zonedDateTime.toString());
    }
}
