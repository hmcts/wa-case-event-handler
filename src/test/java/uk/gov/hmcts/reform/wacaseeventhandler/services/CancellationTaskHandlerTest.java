package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;

import static org.assertj.core.api.Assertions.assertThat;

class CancellationTaskHandlerTest {

    private final CancellationTaskHandler handlerService = new CancellationTaskHandler();

    @Test
    void evaluateDmn() {
        assertThat(handlerService.evaluateDmn(
            EventInformation.builder()
                .jurisdictionId("ia")
                .caseTypeId("asylum")
                .build()))
            .isEmpty();
    }

    @Test
    void handle() throws NoSuchMethodException {
        // change for something meaningful once Cancellation handler is implemented
        assertThat(handlerService.getClass().getMethod("evaluateDmn", EventInformation.class).getName())
            .isEqualTo("evaluateDmn");
    }
}
