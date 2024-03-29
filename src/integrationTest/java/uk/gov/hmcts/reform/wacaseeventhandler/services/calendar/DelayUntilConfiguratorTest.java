package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.Application;
import uk.gov.hmcts.reform.wacaseeventhandler.config.CaffeineConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CalendarResourceInvalidException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CalendarResourceNotFoundException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_NON_WORKING_CALENDAR;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {Application.class, CaffeineConfiguration.class})
@ActiveProfiles({"integration"})
public class DelayUntilConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);
    public static final String NON_WORKING_JSON_OVERRIDE = "https://raw.githubusercontent.com/hmcts/wa-task-management-api/master/src/test/resources/override-working-day-calendar.json";
    public static final String INVALID_CALENDAR_URI = "https://raw.githubusercontent.com/hmcts/wa-task-management-api/895bb18417be056175ec64727e6d5fd39289d489/src/integrationTest/resources/calendars/invalid-calendar.json";
    public static final String DEFAULT_CALENDAR_URI = "https://www.gov.uk/bank-holidays/england-and-wales.json";

    @Autowired
    private DelayUntilConfigurator delayUntilConfigurator;

    @DisplayName("(No 'delayUntilOrigin')")
    @Test
    public void shouldReturnDefaultDelayUntilWhenNoneOfDelayUntilParamsAreAvailable() {

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder().build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.SECONDS));
    }

    @Test
    public void shouldCalculateDelayUntilWhenDefaultDelayUntilWithoutTimeAndTimeAreAvailable() {

        String givenDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntil(givenDelayUntil)
            .delayUntilTime("18:00")
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T18:00");
    }

    @DisplayName("(DelayUntil with delayUntilTime override)")
    @Test
    public void shouldCalculateDelayUntilWhenDefaultDelayUntilWithTimeAndTimeAreAvailable() {

        String givenDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntil(givenDelayUntil + "T21:00")
            .delayUntilTime("18:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T18:00");
    }

    @DisplayName("(No delayUntil but  delayUntilTime exists) - default behavior,"
        + " (No 'delayUntilOrigin' but time override)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDelayUntilTimeIsAvailable() {
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilTime("16:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        String defaultDelayUntil = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime)
            .isEqualTo(defaultDelayUntil + "T16:00");
    }

    @DisplayName(" (DelayUntil but no delayUntilTime override)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDefaultDelayUntilWithTimeIsAvailable() {
        String givenDelayUntil = GIVEN_DATE.plusDays(7).format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntil(givenDelayUntil + "T19:00")
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T19:00");
    }

    @DisplayName("(delayUntil but no delayUntilTime exists)-default behavior missing time element of delayUntil")
    @Test
    public void shouldCalculateDelayUntilWhenDelayUntilWithoutTimeIsAvailable() {
        String givenDelayUntil = GIVEN_DATE.plusDays(7).format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntil(givenDelayUntil)
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        LocalTime now = LocalTime.now();
        // setting minute to 0 so that test don't fail when minute is about to change
        assertThat(localDateTime.withMinute(0))
            .isEqualTo(GIVEN_DATE.plusDays(7).withHour(now.getHour()).withMinute(0));
    }

    @DisplayName(" (No delayUntil  and No delayUntilTime) - default behavior ")
    @Test
    public void shouldReturnDefaultDelayUntilWhenNoDelayUntilPropertiesAreAvailable() {
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(DelayUntilRequest.builder().build());
        assertThat(localDateTime).isCloseTo(LocalDateTime.now(), within(100, ChronoUnit.SECONDS));
    }

    @DisplayName("('delayUntil' and 'delayUntilOrigin')")
    @Test
    public void shouldConsiderDelayUntilWhenBothDelayUntilAndDelayUntilOriginBothProvided() {
        String givenDelayUntil = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String givenDelayUntilOrigin = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntil(givenDelayUntil + "T16:00")
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T16:00");
    }

    @DisplayName("('delayUntilOrigin' and default calculation variables)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDelayUntilOriginIsProvided() {
        String givenDelayUntilOrigin = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilTime(null)
            .delayUntilMustBeWorkingDay(null)
            .delayUntilIntervalDays(null)
            .delayUntilSkipNonWorkingDays(null)
            .delayUntilNonWorkingDaysOfWeek(null)
            .delayUntilNonWorkingCalendar(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isEqualTo(givenDelayUntilOrigin + "T20:00");
    }

    @DisplayName("('delayUntilOrigin' and specified calculation variables - working days considered)\n")
    @ParameterizedTest
    @CsvSource(value = {
        "true,Next,6,8,T18:00",
        "true,Previous,6,8,T18:00",
        "true,No,6,8,T18:00",
        "true,Next,8,12,T18:00",
        "false,Next,6,6,T18:00",
        "false,Next,2,4,T18:00",
        "false,No,6,6,T18:00",
        "false,Previous,2,1,T18:00"
    })
    public void shouldCalculateDateWhenAllDelayUntilOriginPropertiesAreProvidedAndNonWorkingDayNotConsidered(
        Boolean delayUntilSkipNonWorkingDaysFlag,
        String delayUntilMustBeWorkingDayFlag,
        int intervalDays,
        String expectedDays,
        String expectedTime) {
        String givenDelayUntilOrigin = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilIntervalDays(intervalDays)
            .delayUntilNonWorkingCalendar(DEFAULT_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(delayUntilSkipNonWorkingDaysFlag)
            .delayUntilMustBeWorkingDay(delayUntilMustBeWorkingDayFlag)
            .delayUntilTime("18:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        String expectedDelayUntil = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime).isEqualTo(expectedDelayUntil + expectedTime);
    }

    @Test
    void shouldCalculateWithDefaultValuesWhenValueAreNotProvidedExceptIntervalDays() {
        String localDateTime = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(localDateTime + "T20:00")
            .delayUntilIntervalDays(3)
            .delayUntilNonWorkingCalendar(null)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(null)
            .delayUntilMustBeWorkingDay(null)
            .delayUntilTime(null)
            .build();

        LocalDateTime delayUntilDate = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(delayUntilDate).isEqualTo(GIVEN_DATE.plusDays(5).withHour(20));
    }

    @DisplayName("('delayUntil' and specified calculation variables -  Non Working days considered)")
    @Test
    public void shouldCalculateDateWhenNonWorkingDaysConsidered() {
        String givenDelayUntilOrigin = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilIntervalDays(6)
            .delayUntilNonWorkingCalendar(DEFAULT_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("Next")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        String expectedDelayUntil = GIVEN_DATE.plusDays(6)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime).isEqualTo(expectedDelayUntil + "T20:00");
    }

    @DisplayName("('delayUntilOrigin' and specified calculation variables - non working days not considered)")
    @Test
    public void shouldCalculateDateWhenNonWorkingDaysNotConsidered() {
        String givenDelayUntilOrigin = BST_DATE_BACKWARD.format(DATE_FORMATTER);

        //Clocks go back an hour at 2:00am
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T01:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("No")
            .delayUntilTime("02:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo("2022-10-30T02:30");
    }

    @DisplayName("Task delay calculated falls on BST")
    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDelayUntilNonBSTBackword() {
        String givenDelayUntilOrigin = BST_DATE_BACKWARD.format(DATE_FORMATTER);

        //Clocks go back an hour at 2:00am
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T01:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("false")
            .delayUntilTime("02:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo("2022-10-30T02:30");
    }


    @DisplayName("Task delay calculated falls on non BST")
    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDelayUntilNonBSTForward() {
        String givenDelayUntilOrigin = BST_DATE_FORWARD.format(DATE_FORMATTER);

        //Clocks go forward an hour at 1:00am
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("false")
            .delayUntilTime("01:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);

        assertThat(localDateTime).isEqualTo("2023-03-30T01:30");
    }

    @DisplayName("multiple non working calendars URLs")
    @Test
    public void shouldCalculateDateWhenMultipleCalendarsAreProvided() {
        String givenDelayUntilOrigin = LocalDate.of(2022, 12, 26).format(DATE_FORMATTER);

        //Clocks go forward an hour at 1:00am
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR + "," + NON_WORKING_JSON_OVERRIDE)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);


        //27-12-2022 is holiday in england and wales and 30-12-2022 is holiday in second json
        assertThat(localDateTime).isEqualTo("2023-01-04T00:30");
    }

    @Test
    void shouldErrorWhenCalculateDelayUntilContainsInValidCalendar() {
        String givenDelayUntilOrigin = LocalDate.of(2022, 12, 26).format(DATE_FORMATTER);
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR + "," + INVALID_CALENDAR_URI)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        assertThatThrownBy(() -> delayUntilConfigurator.calculateDelayUntil(delayUntilRequest))
            .isInstanceOf(CalendarResourceInvalidException.class)
            .hasMessage("Could not read calendar resource " + INVALID_CALENDAR_URI);
    }

    @Test
    void shouldErrorWhenCalculateDelayUntilContainsWrongUriForCalendar() {
        String givenDelayUntilOrigin = LocalDate.of(2022, 12, 26).format(DATE_FORMATTER);
        String wrongUri = "https://www.gov.uk/bank-holidays/not-a-calendar.json";
        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR + "," + wrongUri)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        assertThatThrownBy(() -> delayUntilConfigurator.calculateDelayUntil(delayUntilRequest))
            .isInstanceOf(CalendarResourceNotFoundException.class)
            .hasMessage("Could not find calendar resource " + wrongUri);
    }

    @Test
    public void shouldCalculateDateWhenIntervalDaysIsLessThan0() {
        String givenDelayUntilOrigin = LocalDate.of(2022, 12, 26).format(DATE_FORMATTER);

        DelayUntilRequest delayUntilRequest = DelayUntilRequest.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(-4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilRequest);
        assertThat(localDateTime).isEqualTo("2022-12-20T00:30");
    }
}
