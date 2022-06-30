package uk.gov.hmcts.reform.wacaseeventhandler.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserIdParserTest {

    @Test
    void should_return_null_when_message_is_null() {
        assertNull(UserIdParser.getUserId(null));
    }

    @Test
    void should_return_null_when_message_string_not_json() {
        assertNull(UserIdParser.getUserId("not a json string"));
    }

    @Test
    void should_return_null_when_message_as_json_contains_no_user_id() {
        assertNull(UserIdParser.getUserId("{}"));
    }

    @Test
    void should_return_user_id_when_message_as_json_contains_user_id() {
        final String userIdValue = "User Id Value";
        assertEquals(userIdValue, UserIdParser.getUserId("{\"UserId\": \"" + userIdValue + "\"}"));
    }
}
