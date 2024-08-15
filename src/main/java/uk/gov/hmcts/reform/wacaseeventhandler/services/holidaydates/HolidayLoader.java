package uk.gov.hmcts.reform.wacaseeventhandler.services.holidaydates;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class HolidayLoader {
    private final GovUkHolidayDatesClient govUkHolidayDatesClient;

    public HolidayLoader(GovUkHolidayDatesClient govUkHolidayDatesClient) {
        this.govUkHolidayDatesClient = govUkHolidayDatesClient;
    }

    @Bean
    public List<LocalDate> loadHolidays() {
        UkHolidayDates holidayDates = govUkHolidayDatesClient.getHolidayDates();
        return holidayDates.getEnglandAndWales().getEvents().stream()
            .map(HolidayDate::getDate)
            .toList();
    }
}
