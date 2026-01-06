package uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wacaseeventhandler.services.calendar.DelayUntilCalculator.DEFAULT_NON_WORKING_CALENDAR;

class BankHolidaysTest {
    @Test
    void should_create_full_object_and_check_dates() throws IOException, URISyntaxException {
        ObjectMapper om = new ObjectMapper();
        BankHolidays bankHolidays = om.readValue(new URI(DEFAULT_NON_WORKING_CALENDAR).toURL(), BankHolidays.class);
        assertEquals("england-and-wales", bankHolidays.getDivision());
        assertFalse(bankHolidays.getEvents().isEmpty());
        assertNotEquals(0, bankHolidays.getEvents().getFirst().hashCode());
        assertNotNull(bankHolidays.getEvents().getFirst());

        assertNotEquals(null, bankHolidays.getEvents().getFirst());
        assertNotEquals(new Object(), bankHolidays.getEvents().getFirst());

        for (BankHolidays.EventDate eventDate: bankHolidays.getEvents()) {
            assertTrue(isValid(eventDate.getDate()));
        }

    }

    public boolean isValid(String dateStr) {
        DateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
