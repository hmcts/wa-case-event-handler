package uk.gov.hmcts.reform.wacaseeventhandler.util;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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
                return userIdNode.textValue();
            }
        } catch (IllegalArgumentException | JacksonException e) {
            log.error("Unable to find User Id in message");
        }
        return null;
    }
}
