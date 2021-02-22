package uk.gov.hmcts.reform.wacaseeventhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

public class CreatorObjectMapper {
    private CreatorObjectMapper() {
    }

    public static String asJsonString(final Object obj) {
        return jsonString(obj, new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE));
    }

    private static String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
