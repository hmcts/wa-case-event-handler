package uk.gov.hmcts.reform.wacaseeventhandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.LoggingUtilityFailure;

public final class LoggingUtility {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .registerModule(new JavaTimeModule())
        .registerModule(new Jdk8Module());

    public static String logPrettyPrint(String str) {
        try {
            Object json = MAPPER.readValue(str, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new LoggingUtilityFailure("Error logging pretty print: " + str, e);
        }
    }

    public static String logPrettyPrint(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new LoggingUtilityFailure("Error logging pretty print: " + obj, e);
        }
    }

    private LoggingUtility() {
        // utility class should not have a public or default constructor
    }
}
