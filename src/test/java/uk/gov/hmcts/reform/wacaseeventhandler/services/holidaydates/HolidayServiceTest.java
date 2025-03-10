package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayServiceTest {

    private HolidayService holidayService;

    private static final List<LocalDate> holidayList = List.of(
        LocalDate.of(2022, Month.DECEMBER, 25),
        LocalDate.of(2022, Month.JANUARY, 1),
        LocalDate.of(2022, Month.APRIL, 15)
    );

    @BeforeEach
    void setup() {
        holidayService = new HolidayService(holidayList);
    }

    @Test
    void testIsHolidayZoneDateTimeReturnsTrue() {
        holidayList.stream()
            .map(localDate -> ZonedDateTime.of(localDate.atTime(11,30), ZoneId.systemDefault()))
            .forEach(zonedDateTime -> assertTrue(holidayService.isHoliday(zonedDateTime)));
    }

    @Test
    void testIsHolidayZoneDateTimeReturnsFalse() {
        ZonedDateTime zonedDateTime =
            ZonedDateTime.of(
                LocalDateTime.of(2022, Month.SEPTEMBER, 8, 13, 16), ZoneId.systemDefault());
        assertFalse(holidayService.isHoliday(zonedDateTime));
    }

    @Test
    void testIsHolidayLocalDateReturnsTrue() {
        holidayList.forEach(localDate -> assertTrue(holidayService.isHoliday(localDate)));
    }

    @Test
    void testIsHolidayLocalDateReturnsFalse() {
        LocalDate localDate = LocalDate.of(2022, Month.SEPTEMBER, 8);
        assertFalse(holidayService.isHoliday(localDate));
    }

    @Test
    void testIsWeekendZonedDateTimeReturnsTrue() {
        ZonedDateTime saturdaySeptemberTenth =
            ZonedDateTime.of(
                LocalDateTime.of(2022, Month.SEPTEMBER, 10, 13, 16), ZoneId.systemDefault());

        ZonedDateTime sundaySeptemberEleventh =
            ZonedDateTime.of(
                LocalDateTime.of(2022, Month.SEPTEMBER, 11, 13, 16), ZoneId.systemDefault());

        assertTrue(holidayService.isWeekend(saturdaySeptemberTenth));
        assertTrue(holidayService.isWeekend(sundaySeptemberEleventh));
    }

    @Test
    void testIsWeekendZoneDateTimeReturnsFalse() {
        ZonedDateTime fridaySeptemberNinth =
            ZonedDateTime.of(
                LocalDateTime.of(2022, Month.SEPTEMBER, 9, 13, 16), ZoneId.systemDefault());

        assertFalse(holidayService.isWeekend(fridaySeptemberNinth));
    }

    @Test
    void testIsWeekendLocalDateReturnsTrue() {
        LocalDate saturdaySeptemberTenth = LocalDate.of(2022, Month.SEPTEMBER, 10);
        LocalDate sundaySeptemberEleventh = LocalDate.of(2022, Month.SEPTEMBER, 11);

        assertTrue(holidayService.isWeekend(saturdaySeptemberTenth));
        assertTrue(holidayService.isWeekend(sundaySeptemberEleventh));
    }

    @Test
    void testIsWeekendLocalDateReturnsFalse() {
        LocalDate fridaySeptemberNinth = LocalDate.of(2022, Month.SEPTEMBER, 9);

        assertFalse(holidayService.isWeekend(fridaySeptemberNinth));
    }
}
