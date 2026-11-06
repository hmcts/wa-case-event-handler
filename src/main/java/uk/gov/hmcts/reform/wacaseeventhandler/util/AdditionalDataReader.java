package uk.gov.hmcts.reform.wacaseeventhandler.util;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.AdditionalData;

import java.util.Map;

@Slf4j
public final class AdditionalDataReader {

    private AdditionalDataReader() {

    }

    public static Map readValue(ObjectMapper objectMapper, AdditionalData additionalData) {
        if (additionalData != null) {
            return objectMapper.convertValue(additionalData, Map.class);
        }
        return null;
    }
}
