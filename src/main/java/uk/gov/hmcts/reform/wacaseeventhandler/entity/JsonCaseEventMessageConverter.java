package uk.gov.hmcts.reform.wacaseeventhandler.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.exceptions.CaseEventMessageInvalidJsonException;

import java.io.IOException;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class JsonCaseEventMessageConverter implements AttributeConverter<JsonNode, String> {
    private static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String convertToDatabaseColumn(final JsonNode objectValue) {
        if (objectValue == null) {
            return null;
        }
        return objectValue.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(final String dataValue) {
        try {
            if (dataValue == null) {
                return null;
            }
            return mapper.readTree(dataValue);
        } catch (IOException e) {
            throw new CaseEventMessageInvalidJsonException("Unable to deserialize to json field", e);
        }
    }
}
