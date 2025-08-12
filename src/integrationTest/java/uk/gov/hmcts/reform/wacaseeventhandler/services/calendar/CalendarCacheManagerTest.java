package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.Application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_NON_WORKING_CALENDAR;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CalendarCacheManagerTest.TestConfiguration.class)
@ActiveProfiles({"integration"})
public class CalendarCacheManagerTest {

    @Autowired
    private DelayUntilConfigurator delayUntilConfigurator;

    @SpyBean
    private PublicHolidayService publicHolidayService;

    @DisplayName("(Access calendars successfully and retrieve results which are also cached)")
    @Test
    public void shouldCacheThePublicHolidayCalendarResponse() {
        String givenDelayUntilOrigin = LocalDate.of(2026, 12, 25).format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isEqualTo("2027-01-04T00:30");
        verify(publicHolidayService, times(1)).getPublicHolidays(DEFAULT_NON_WORKING_CALENDAR);
    }

    @DisplayName("(Access calendars successfully if cached information is not available)")
    @Test
    public void shouldCallHolidayServiceIfCashExpires() {
        String givenDelayUntilOrigin = LocalDate.of(2026, 12, 25).format(DATE_FORMATTER);

        TestConfiguration.fakeTicker.advance(25, TimeUnit.HOURS);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isEqualTo("2027-01-04T00:30");
        verify(publicHolidayService, times(1)).getPublicHolidays(DEFAULT_NON_WORKING_CALENDAR);
    }

    @Configuration
    @Import(Application.class)
    public static class TestConfiguration {

        static FakeTicker fakeTicker = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return fakeTicker::read;
        }

    }

}
