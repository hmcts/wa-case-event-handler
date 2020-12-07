package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

import static org.assertj.core.api.Assertions.assertThat;

class WarningTaskHandlerTest {

    private final WarningTaskHandler handlerService = new WarningTaskHandler();

    @Test
    void can_handle() {
        assertThat(handlerService.canHandle(EventInformation.builder().build())).isFalse();
    }

    @Test
    void handle() throws NoSuchMethodException {
        assertThat(handlerService.getClass().getMethod("handle").getName()).isEqualTo("handle");
    }

}
