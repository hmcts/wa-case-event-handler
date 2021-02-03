package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates.HolidayService;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.of;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("PMD.JUnitAssertionsShouldIncludeMessage")
class HolidayServiceTest {
    @Test
    void isNotHoliday() {
        ZonedDateTime date = of(2020, 9, 1, 1, 2, 3, 4, systemDefault());
        boolean isHoliday = new HolidayService(singletonList(LocalDate.of(2020, 9, 2))).isHoliday(date);

        assertThat(isHoliday, is(false));
    }

    @Test
    void isHoliday() {
        ZonedDateTime date = of(2020, 9, 1, 1, 2, 3, 4, systemDefault());
        boolean isHoliday = new HolidayService(singletonList(LocalDate.of(2020, 9, 1))).isHoliday(date);

        assertThat(isHoliday, is(true));
    }

}
