package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import feign.FeignException;
import feign.codec.DecodeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.SnakeCaseFeignConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar.BankHolidays;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CalendarResourceInvalidException;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CalendarResourceNotFoundException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores all public holidays for england and wales retrieved from Gov uk API: https://www.gov.uk/bank-holidays/england-and-wales.json .
 */
@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
@Import(SnakeCaseFeignConfiguration.class)
public class PublicHolidaysCollection {
    private final PublicHolidayService publicHolidayService;

    public PublicHolidaysCollection(PublicHolidayService publicHolidayService) {
        this.publicHolidayService = publicHolidayService;
    }

    public Set<LocalDate> getPublicHolidays(List<String> uris) {
        List<BankHolidays.EventDate> events = new ArrayList<>();
        BankHolidays allPublicHolidays = BankHolidays.builder().events(events).build();
        if (uris != null) {
            for (String uri : uris) {
                try {
                    BankHolidays publicHolidays = publicHolidayService.getPublicHolidays(uri);
                    processCalendar(publicHolidays, allPublicHolidays);
                } catch (DecodeException e) {
                    log.error("Could not read calendar resource {}", uri, e);
                    throw new CalendarResourceInvalidException("Could not read calendar resource " + uri, e);
                } catch (FeignException e) {
                    log.error("Could not find calendar resource {}", uri, e);
                    throw new CalendarResourceNotFoundException("Could not find calendar resource " + uri, e);
                }
            }
        }


        return allPublicHolidays.getEvents().stream()
            .map(item -> LocalDate.parse(item.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd")))
            .collect(Collectors.toSet());
    }

    private void processCalendar(BankHolidays publicHolidays, BankHolidays allPublicHolidays) {
        for (BankHolidays.EventDate eventDate : publicHolidays.getEvents()) {
            if (eventDate.isWorkingDay()) {
                if (allPublicHolidays.getEvents().contains(eventDate)) {
                    allPublicHolidays.getEvents().remove(eventDate);
                }
            } else {
                allPublicHolidays.getEvents().add(eventDate);
            }
        }
    }
}
