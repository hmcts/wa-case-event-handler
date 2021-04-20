package uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;

@ToString
@EqualsAndHashCode
@Builder
public class AdditionalData {

    private Map<String, JsonNode> data;
    private Map<String, JsonNode> definition;

    @JsonCreator
    public AdditionalData(@JsonProperty("Data") Map<String, JsonNode> data,
                          @JsonProperty("Definition") Map<String, JsonNode> definition) {
        this.data = data;
        this.definition = definition;
    }

    public Map<String, JsonNode> getData() {
        return data;
    }

    public Map<String, JsonNode> getDefinition() {
        return definition;
    }
}
