package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationTaskHandlerServiceImplTest {

    private final CancellationTaskHandlerServiceImpl handlerService = new CancellationTaskHandlerServiceImpl();

    @Test
    void can_handle() {
        assertThat(handlerService.canHandle()).isFalse();
    }

}
