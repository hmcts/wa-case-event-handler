package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DATE_FORMATTER;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_DATE_TIME;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_NON_WORKING_CALENDAR;

@SpringBootTest
@ActiveProfiles({"integration"})
public class DelayUntilConfiguratorTest {

    public static final LocalDateTime GIVEN_DATE = LocalDateTime.of(2022, 10, 13, 18, 0, 0);
    public static final LocalDateTime BST_DATE_BACKWARD = LocalDateTime.of(2022, 10, 26, 18, 0, 0);
    public static final LocalDateTime BST_DATE_FORWARD = LocalDateTime.of(2023, 3, 26, 18, 0, 0);
    public static final String NON_WORKING_JSON_OVERRidE = "https://raw.githubusercontent.com/hmcts/wa-task-management-api/master/src/test/resources/override-working-day-calendar.json";

    @Autowired
    private DelayUntilConfigurator delayUntilConfigurator;


    @DisplayName("(No 'delayUntilOrigin')")
    @Test
    public void shouldReturnDefaultDelayUntilWhenNoneOfDelayUntilParamsAreAvailable() {

        DelayUntilObject delayUntilObject = DelayUntilObject.builder().build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);
        assertThat(localDateTime).isEqualTo(DEFAULT_DATE_TIME);
    }

    @Test
    public void shouldCalculateDelayUntilWhenDefaultDelayUntilWithoutTimeAndTimeAreAvailable() {

        String givenDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(givenDelayUntil)
            .delayUntilTime("18:00")
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T18:00");
    }

    @DisplayName("(DelayUntil with delayUntilTime override)")
    @Test
    public void shouldCalculateDelayUntilWhenDefaultDelayUntilWithTimeAndTimeAreAvailable() {

        String givenDelayUntil = GIVEN_DATE.format(DATE_FORMATTER);
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(givenDelayUntil + "T21:00")
            .delayUntilTime("18:00")
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T18:00");
    }

    @DisplayName("(No delayUntil but  delayUntilTime exists) - default behavior,"
        + " (No 'delayUntilOrigin' but time override)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDelayUntilTimeIsAvailable() {
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilTime("16:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);
        String defaultDelayUntil = DEFAULT_DATE_TIME.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime)
            .isEqualTo(defaultDelayUntil + "T16:00");
    }

    @DisplayName(" (DelayUntil but no delayUntilTime override)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDefaultDelayUntilWithTimeIsAvailable() {
        String givenDelayUntil = GIVEN_DATE.plusDays(7).format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(givenDelayUntil + "T19:00")
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T19:00");
    }

    @DisplayName("(delayUntil but no delayUntilTime exists)-default behavior missing time element of delayUntil")
    @Test
    public void shouldCalculateDelayUntilWhenDelayUntilWithoutTimeIsAvailable() {
        String givenDelayUntil = GIVEN_DATE.plusDays(7).format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(givenDelayUntil)
            .build();
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        LocalTime now = LocalTime.now();
        // setting minute to 0 so that test don't fail when minute is about to change
        assertThat(localDateTime.withMinute(0))
            .isEqualTo(GIVEN_DATE.plusDays(7).withHour(now.getHour()).withMinute(0));
    }

    @DisplayName(" (No delayUntil  and No delayUntilTime) - default behavior ")
    @Test
    public void shouldReturnDefaultDelayUntilWhenNoDelayUntilPropertiesAreAvailable() {
        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(DelayUntilObject.builder().build());
        assertThat(localDateTime).isEqualTo(DEFAULT_DATE_TIME);
    }

    @DisplayName("('delayUntil' and 'delayUntilOrigin')")
    @Test
    public void shouldConsiderDelayUntilWhenBothDelayUntilAndDelayUntilOriginBothProvided() {
        String givenDelayUntil = GIVEN_DATE.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String givenDelayUntilOrigin = GIVEN_DATE.plusDays(4).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntil(givenDelayUntil + "T16:00")
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);
        assertThat(localDateTime).isEqualTo(givenDelayUntil + "T16:00");
    }

    @DisplayName("('delayUntilOrigin' and default calculation variables)")
    @Test
    public void shouldCalculateDelayUntilWhenOnlyDelayUntilOriginIsProvided() {
        String givenDelayUntilOrigin = GIVEN_DATE.plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilTime(null)
            .delayUntilMustBeWorkingDay(null)
            .delayUntilIntervalDays(null)
            .delayUntilSkipNonWorkingDays(null)
            .delayUntilNonWorkingDaysOfWeek(null)
            .delayUntilNonWorkingCalendar(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);
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

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilIntervalDays(intervalDays)
            .delayUntilNonWorkingCalendar("https://www.gov.uk/bank-holidays/england-and-wales.json")
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(delayUntilSkipNonWorkingDaysFlag)
            .delayUntilMustBeWorkingDay(delayUntilMustBeWorkingDayFlag)
            .delayUntilTime("18:00")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        String expectedDelayUntil = GIVEN_DATE.plusDays(Integer.parseInt(expectedDays))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime).isEqualTo(expectedDelayUntil + expectedTime);
    }

    @Test
    void shouldCalculateWithDefaultValuesWhenValueAreNotProvidedExceptIntervalDays() {
        String localDateTime = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(localDateTime + "T20:00")
            .delayUntilIntervalDays(3)
            .delayUntilNonWorkingCalendar(null)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(null)
            .delayUntilMustBeWorkingDay(null)
            .delayUntilTime(null)
            .build();

        LocalDateTime delayUntilDate = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);
        assertThat(delayUntilDate).isEqualTo(GIVEN_DATE.plusDays(5).withHour(20));
    }

    @DisplayName("('delayUntil' and specified calculation variables -  Non Working days considered)")
    @Test
    public void shouldCalculateDateWhenNonWorkingDaysConsidered() {
        String givenDelayUntilOrigin = GIVEN_DATE.format(DATE_FORMATTER);

        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T20:00")
            .delayUntilIntervalDays(6)
            .delayUntilNonWorkingCalendar("https://www.gov.uk/bank-holidays/england-and-wales.json")
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("Next")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        String expectedDelayUntil = GIVEN_DATE.plusDays(6)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        assertThat(localDateTime).isEqualTo(expectedDelayUntil + "T20:00");
    }

    @DisplayName("('delayUntilOrigin' and specified calculation variables - non working days not considered)")
    @Test
    public void shouldCalculateDateWhenNonWorkingDaysNotConsidered() {
        String givenDelayUntilOrigin = BST_DATE_BACKWARD.format(DATE_FORMATTER);

        //Clocks go back an hour at 2:00am
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T01:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar("https://www.gov.uk/bank-holidays/england-and-wales.json")
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("No")
            .delayUntilTime("02:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo("2022-10-30T02:30");
    }

    @DisplayName("Task delay calculated falls on BST")
    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDelayUntilNonBSTBackword() {
        String givenDelayUntilOrigin = BST_DATE_BACKWARD.format(DATE_FORMATTER);

        //Clocks go back an hour at 2:00am
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T01:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar("https://www.gov.uk/bank-holidays/england-and-wales.json")
            .delayUntilNonWorkingDaysOfWeek("")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("false")
            .delayUntilTime("02:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo("2022-10-30T02:30");
    }


    @DisplayName("Task delay calculated falls on non BST")
    @Test
    public void shouldCalculateDateWhenOriginDateIsBSTDelayUntilNonBSTForward() {
        String givenDelayUntilOrigin = BST_DATE_FORWARD.format(DATE_FORMATTER);

        //Clocks go forward an hour at 1:00am
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar("https://www.gov.uk/bank-holidays/england-and-wales.json")
            .delayUntilNonWorkingDaysOfWeek("")
            .delayUntilSkipNonWorkingDays(false)
            .delayUntilMustBeWorkingDay("false")
            .delayUntilTime("01:30")
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);

        assertThat(localDateTime).isEqualTo("2023-03-30T01:30");
    }

    @DisplayName("multiple non working calendars URLs")
    @Test
    public void shouldCalculateDateWhenMultipleCalendarsAreProvided() {
        String givenDelayUntilOrigin = LocalDate.of(2022, 12, 26).format(DATE_FORMATTER);

        //Clocks go forward an hour at 1:00am
        DelayUntilObject delayUntilObject = DelayUntilObject.builder()
            .delayUntilOrigin(givenDelayUntilOrigin + "T00:30")
            .delayUntilIntervalDays(4)
            .delayUntilNonWorkingCalendar(DEFAULT_NON_WORKING_CALENDAR + "," + NON_WORKING_JSON_OVERRidE)
            .delayUntilNonWorkingDaysOfWeek("SATURDAY,SUNDAY")
            .delayUntilSkipNonWorkingDays(true)
            .delayUntilMustBeWorkingDay("Next")
            .delayUntilTime(null)
            .build();

        LocalDateTime localDateTime = delayUntilConfigurator.calculateDelayUntil(delayUntilObject);


        //27-12-2022 is holiday in england and wales and 30-12-2022 is holiday in second json
        assertThat(localDateTime).isEqualTo("2023-01-04T00:30");
    }
}
