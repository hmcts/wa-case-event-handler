package uk.gov.hmcts.reform.wacaseeventhandler.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class UserIdParser {
    private static final String USER_ID = "UserId";

    private UserIdParser() {

    }

    public static String getUserId(final String message) {
        try {
            JsonNode messageAsJson = new ObjectMapper().readTree(message);
            final JsonNode userIdNode = messageAsJson.findPath(USER_ID);
            if (!userIdNode.isMissingNode()) {
                String userIdTextValue = userIdNode.textValue();
                log.info("Returning User Id {} found in message", userIdTextValue);
                return userIdTextValue;
            }
        } catch (IllegalArgumentException | JsonProcessingException e) {
            log.info("Unable to find User Id in message");
        }
        return null;
    }
}
