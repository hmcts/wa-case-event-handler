package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.InitiationTaskHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerControllerTest {

    @Mock
    private InitiationTaskHandler initiationTaskHandler;

    @Test
    void caseEventHandler() {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        CaseEventHandlerController controller = new CaseEventHandlerController(handlerServices);

        ResponseEntity<Void> response = controller.caseEventHandler(
            EventInformation.builder()
                .jurisdictionId("ia")
                .caseTypeId("asylum")
                .build()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }
}
