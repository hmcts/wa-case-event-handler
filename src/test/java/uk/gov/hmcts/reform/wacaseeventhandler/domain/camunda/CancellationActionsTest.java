package uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationActionsTest {


    @Test
    void should_resolve_to_enum() {

        assertEquals(CancellationActions.CANCEL, CancellationActions.from("cancel"));
        assertEquals(CancellationActions.CANCEL, CancellationActions.from("Cancel"));
        assertEquals(CancellationActions.CANCEL, CancellationActions.from("CANCEL"));

        assertEquals(CancellationActions.WARN, CancellationActions.from("warn"));
        assertEquals(CancellationActions.WARN, CancellationActions.from("Warn"));
        assertEquals(CancellationActions.WARN, CancellationActions.from("WARN"));

        assertEquals(CancellationActions.RECONFIGURE, CancellationActions.from("RECONFIGURE"));
        assertEquals(CancellationActions.RECONFIGURE, CancellationActions.from("reconfigure"));
        assertEquals(CancellationActions.RECONFIGURE, CancellationActions.from("Reconfigure"));
    }

    @Test
    void if_this_test_fails_it_is_because_it_needs_updating_with_your_changes() {
        assertEquals(3, CancellationActions.values().length);
    }
}
