package uk.gov.hmcts.reform.wacaseeventhandler;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

public class CreatorObjectMapper {
    private CreatorObjectMapper() {
    }

    public static String asJsonString(final Object obj) {
        return jsonString(obj, JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)
            .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build());
    }

    private static String jsonString(Object obj, ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
