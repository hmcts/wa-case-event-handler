package uk.gov.hmcts.reform.wacaseeventhandler.services.dates;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class IsoDateFormatterTest {

    @Test
    void format() {
        IsoDateFormatter isoDateFormatter = new IsoDateFormatter();

        String fixedDate = "2020-12-16T15:56:19.403975";

        String actual = isoDateFormatter.format(LocalDateTime.parse(fixedDate));

        assertThat(actual).isEqualTo("2020-12-16T15:56:19.403975Z");
    }
}
