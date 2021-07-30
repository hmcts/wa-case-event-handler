package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.StringUtils;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Slf4j
@EqualsAndHashCode
@ToString
@NoArgsConstructor
public class WarningValues {

    private List<Warning> values = new ArrayList<>();

    public WarningValues(List<Warning> values) {
        requireNonNull(values);
        this.values = values;
    }

    public WarningValues(String values) {
        requireNonNull(values);
        try {
            this.values = new ObjectMapper().reader()
                .forType(new TypeReference<List<Warning>>() {
                })
                .readValue(values);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Could not deserialize values");
        }
    }

    public List<Warning> getValues() {
        return values;
    }

    public String getValuesAsJson() {
        try {
            return new ObjectMapper().writeValueAsString(values);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Could not deserialize Waring value");
        }
        return StringUtils.EMPTY;
    }
}
