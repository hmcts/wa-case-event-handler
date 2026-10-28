package uk.gov.hmcts.reform.wacaseeventhandler.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.AnnotationIntrospector;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.LoggingUtilityFailure;

public final class LoggingUtility {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .annotationIntrospector(loggingIntrospector())
        .build();

    public static String logPrettyPrint(String str) {
        try {
            Object json = MAPPER.readValue(str, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JacksonException e) {
            throw new LoggingUtilityFailure("Error logging pretty print: " + str, e);
        }
    }

    public static String logPrettyPrint(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new LoggingUtilityFailure("Error logging pretty print: " + obj, e);
        }
    }

    private LoggingUtility() {
        // utility class should not have a public or default constructor
    }

    /**
     * Jackson 3 applies constructor {@code @JsonProperty} names when serialising.
     * Logging previously used getter names, so creator parameter names are ignored here.
     */
    private static AnnotationIntrospector loggingIntrospector() {
        return new JacksonAnnotationIntrospector() {
            @Override
            public PropertyName findNameForSerialization(MapperConfig<?> config, Annotated annotated) {
                if (annotated instanceof AnnotatedParameter) {
                    return null;
                }
                return super.findNameForSerialization(config, annotated);
            }

            @Override
            public PropertyName findNameForDeserialization(MapperConfig<?> config, Annotated annotated) {
                if (annotated instanceof AnnotatedParameter) {
                    return null;
                }
                return super.findNameForDeserialization(config, annotated);
            }
        };
    }
}
