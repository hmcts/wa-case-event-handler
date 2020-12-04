package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WarningTaskHandlerServiceImplTest {

    private final InitiationTaskHandler handlerService = new InitiationTaskHandler();

    @Test
    void can_handle() {
        assertThat(handlerService.canHandle()).isFalse();
    }

    @Test
    void handle() throws NoSuchMethodException {
        assertThat(handlerService.getClass().getMethod("handle").getName()).isEqualTo("handle");
    }

}
