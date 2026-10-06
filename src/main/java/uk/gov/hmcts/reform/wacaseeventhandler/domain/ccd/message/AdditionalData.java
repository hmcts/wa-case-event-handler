package uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.jackson.databind.JsonNode;

import java.util.Map;

@ToString
@EqualsAndHashCode
@Builder
public class AdditionalData {

    private final Map<String, Object> data;
    private final Map<String, JsonNode> definition;

    @JsonCreator
    public AdditionalData(@JsonProperty("Data") Map<String, Object> data,
                          @JsonProperty("Definition") Map<String, JsonNode> definition) {
        this.data = data;
        this.definition = definition;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, JsonNode> getDefinition() {
        return definition;
    }
}
