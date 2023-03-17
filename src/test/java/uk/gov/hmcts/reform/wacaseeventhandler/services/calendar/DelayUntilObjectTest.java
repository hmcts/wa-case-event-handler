package uk.gov.hmcts.reform.wacaseeventhandler.services.calendar;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.base.Splitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DelayUntilObjectTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapStringToObject() throws IOException {
        String jsonString = "{delayUntilIntervalDays=4, delayUntilNonWorkingCalendar=https://www.gov.uk/bank-holidays/england-and-wales.json, delayUntilSkipNonWorkingDays=No, delayUntilOrigin=2023-03-17T18:00, delayUntilNonWorkingDaysOfWeek=SATURDAY,SUNDAY, delayUntilMustBeWorkingDay=true}";

        Map<String, String> split = Arrays.stream(jsonString.replaceAll("[{}]", "").split(", "))
            .map(s -> s.split("=", 2))
            .collect(Collectors.toMap(a -> a[0].trim(), a -> a[1].trim()));

        System.out.println(split);
        DelayUntilObject delayUntilObject = objectMapper.convertValue(split, DelayUntilObject.class);
        System.out.println(delayUntilObject.getDelayUntilOrigin());
        System.out.println(delayUntilObject.getDelayUntilIntervalDays());


    }

    @Test
    void shouldMapStringToObject1() throws IOException {
        String jsonString = "{delayUntilIntervalDays=4, delayUntilNonWorkingCalendar=https://www.gov.uk/bank-holidays/england-and-wales.json, delayUntilSkipNonWorkingDays=No, delayUntilOrigin=2023-03-17T18:00, delayUntilNonWorkingDaysOfWeek=SATURDAY,SUNDAY, delayUntilMustBeWorkingDay=true}";

        jsonString = jsonString.replace("=", "\":\"")
            .replace("{", "{\"").replace("}", "\"}")
//            .replace(" ", "");
        System.out.println(jsonString);
        jsonString = jsonString.replace(", ", "\", \"");
        System.out.println(jsonString);
        System.out.println(jsonString);

        DelayUntilObject delayUntilObject = objectMapper.readValue(jsonString, DelayUntilObject.class);
        System.out.println(delayUntilObject.getDelayUntilOrigin());
        System.out.println(delayUntilObject.getDelayUntilIntervalDays());


    }
}
