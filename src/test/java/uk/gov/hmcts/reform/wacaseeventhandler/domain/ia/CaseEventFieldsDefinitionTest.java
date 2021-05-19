package uk.gov.hmcts.reform.wacaseeventhandler.domain.ia;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CaseEventFieldsDefinitionTest {

    @Test
    void has_correct_values() {
        assertEquals("appealType", CaseEventFieldsDefinition.APPEAL_TYPE.value());
        assertEquals("lastModifiedDirection", CaseEventFieldsDefinition.LAST_MODIFIED_DIRECTION.value());
        assertEquals("dateDue", CaseEventFieldsDefinition.DATE_DUE.value());
    }

    @Test
    void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(3, CaseEventFieldsDefinition.values().length);
    }
}
