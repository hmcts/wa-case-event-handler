package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningTaskHandler;

import static org.assertj.core.api.Assertions.assertThat;

class WarningTaskHandlerTest {

    private final WarningTaskHandler handlerService = new WarningTaskHandler();

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
        // change for something meaningful once Warning handler is implemented
        assertThat(handlerService.getClass().getMethod("evaluateDmn", EventInformation.class).getName())
            .isEqualTo("evaluateDmn");
    }

}
