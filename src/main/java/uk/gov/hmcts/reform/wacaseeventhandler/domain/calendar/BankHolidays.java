package uk.gov.hmcts.reform.wacaseeventhandler.domain.calendar;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Builder
public class BankHolidays {


    @JsonProperty("division")
    String division;

    @JsonProperty("events")
    List<EventDate> events;

    public BankHolidays() {
        //empty constructor for json conversion
    }

    public BankHolidays(String division, List<EventDate> events) {
        this.division = division;
        this.events = events;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Builder
    public static class EventDate {

        @JsonProperty("date")
        String date;
        @JsonProperty("working_day")
        boolean workingDay;

        public EventDate() {
            //empty constructor for json conversion
        }

        public EventDate(String date, boolean workingDay) {
            this.date = date;
            this.workingDay = workingDay;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            EventDate eventDate = (EventDate) object;
            return date.equals(eventDate.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date);
        }
    }


}
