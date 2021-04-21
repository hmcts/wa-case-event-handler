package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;
import javax.validation.constraints.NotEmpty;

@ToString
@EqualsAndHashCode
@Builder
public final class AdditionalData {

    @NotEmpty
    private final Map<String, String> data;
    @NotEmpty
    private final Map<String, Object> definition;

    @JsonCreator
    public AdditionalData(@JsonProperty("Data") Map<String, String> data,
                          @JsonProperty("Definition") Map<String, Object> definition) {
        this.data = data;
        this.definition = definition;
    }

    public Map<String, String> getData() {
        return data;
    }

    public Map<String, Object> getDefinition() {
        return definition;
    }
}
