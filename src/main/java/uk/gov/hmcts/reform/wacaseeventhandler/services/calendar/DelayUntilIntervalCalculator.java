package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.DelayUntilIntervalData;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.DelayUntilIntervalData.MUST_BE_WORKING_DAY_NEXT;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.DelayUntilIntervalData.MUST_BE_WORKING_DAY_PREVIOUS;

@Slf4j
@Component
public class DelayUntilIntervalCalculator implements DelayUntilCalculator {
    final WorkingDayIndicator workingDayIndicator;

    public DelayUntilIntervalCalculator(WorkingDayIndicator workingDayIndicator) {
        this.workingDayIndicator = workingDayIndicator;
    }

    @Override
    public boolean supports(DelayUntilObject delayUntilObject) {

        return Optional.ofNullable(delayUntilObject.getDelayUntilOrigin()).isPresent()
            && Optional.ofNullable(delayUntilObject.getDelayUntil()).isEmpty();
    }

    @Override
    public LocalDateTime calculateDate(DelayUntilObject delayUntilObject) {
        DelayUntilIntervalData delayUntilIntervalData = readDateTypeOriginFields(delayUntilObject);

        LocalDateTime referenceDate = delayUntilIntervalData.getReferenceDate();
        LocalDate localDate = referenceDate.toLocalDate();
        if (delayUntilIntervalData.isSkipNonWorkingDays()) {
            localDate = skipNonWorkingDays(delayUntilIntervalData, localDate);
        } else {
            localDate = considerAllDaysAsWorking(delayUntilIntervalData, localDate);
        }

        return calculateTime(delayUntilIntervalData.getDelayUntilTime(), referenceDate, localDate);
    }

    private LocalDate considerAllDaysAsWorking(DelayUntilIntervalData delayUntilIntervalData, LocalDate localDate) {
        LocalDate calculatedDate = localDate.plusDays(delayUntilIntervalData.getIntervalDays());
        boolean workingDay = workingDayIndicator.isWorkingDay(
            calculatedDate,
            delayUntilIntervalData.getNonWorkingCalendars(),
            delayUntilIntervalData.getNonWorkingDaysOfWeek()
        );
        if (delayUntilIntervalData.getMustBeWorkingDay()
            .equalsIgnoreCase(MUST_BE_WORKING_DAY_NEXT) && !workingDay) {
            calculatedDate = workingDayIndicator.getNextWorkingDay(
                calculatedDate,
                delayUntilIntervalData.getNonWorkingCalendars(),
                delayUntilIntervalData.getNonWorkingDaysOfWeek()
            );
        }
        if (delayUntilIntervalData.getMustBeWorkingDay()
            .equalsIgnoreCase(MUST_BE_WORKING_DAY_PREVIOUS) && !workingDay) {
            calculatedDate = workingDayIndicator.getPreviousWorkingDay(
                calculatedDate,
                delayUntilIntervalData.getNonWorkingCalendars(),
                delayUntilIntervalData.getNonWorkingDaysOfWeek()
            );
        }
        return calculatedDate;
    }

    private LocalDate skipNonWorkingDays(DelayUntilIntervalData delayUntilIntervalData, LocalDate localDate) {
        LocalDate calculatedDate = localDate;
        if (delayUntilIntervalData.getIntervalDays() < 0) {
            for (long counter = delayUntilIntervalData.getIntervalDays(); counter < 0; counter++) {
                calculatedDate = workingDayIndicator.getPreviousWorkingDay(
                    calculatedDate,
                    delayUntilIntervalData.getNonWorkingCalendars(),
                    delayUntilIntervalData.getNonWorkingDaysOfWeek()
                );
            }
        } else {
            for (int counter = 0; counter < delayUntilIntervalData.getIntervalDays(); counter++) {
                calculatedDate = workingDayIndicator.getNextWorkingDay(
                    calculatedDate,
                    delayUntilIntervalData.getNonWorkingCalendars(),
                    delayUntilIntervalData.getNonWorkingDaysOfWeek()
                );
            }
        }
        return calculatedDate;
    }

    private LocalDateTime calculateTime(String dateTypeTime, LocalDateTime referenceDate, LocalDate calculateDate) {
        LocalTime baseReferenceTime = referenceDate.toLocalTime();
        LocalDateTime dateTime = calculateDate.atTime(baseReferenceTime);

        if (Optional.ofNullable(dateTypeTime).isPresent()) {
            dateTime = calculateDate.atTime(LocalTime.parse(dateTypeTime));
        }
        return dateTime;
    }

    private DelayUntilIntervalData readDateTypeOriginFields(DelayUntilObject delayUntilObject) {
        return DelayUntilIntervalData.builder()
            .referenceDate(Optional.ofNullable(delayUntilObject.getDelayUntilOrigin())
                               .map(this::parseDateTime)
                               .orElse(DEFAULT_DATE_TIME))
            .intervalDays(Optional.ofNullable(delayUntilObject.getDelayUntilIntervalDays())
                              .map(Long::valueOf)
                              .orElse(0L))
            .nonWorkingCalendars(Optional.ofNullable(delayUntilObject.getDelayUntilNonWorkingCalendar())
                                     .map(s -> s.split(","))
                                     .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                     .map(Arrays::asList)
                                     .orElse(List.of(DEFAULT_NON_WORKING_CALENDAR)))
            .nonWorkingDaysOfWeek(Optional.ofNullable(delayUntilObject.getDelayUntilNonWorkingDaysOfWeek())
                                      .map(s -> s.split(","))
                                      .map(a -> Arrays.stream(a).map(String::trim).toArray(String[]::new))
                                      .map(Arrays::asList)
                                      .orElse(List.of()))
            .skipNonWorkingDays(Optional.ofNullable(delayUntilObject.getDelayUntilSkipNonWorkingDays())
                                    .orElse(true))
            .mustBeWorkingDay(Optional.ofNullable(delayUntilObject.getDelayUntilMustBeWorkingDay())
                                  .orElse(MUST_BE_WORKING_DAY_NEXT))
            .delayUntilTime(delayUntilObject.getDelayUntilTime())
            .build();
    }
}
