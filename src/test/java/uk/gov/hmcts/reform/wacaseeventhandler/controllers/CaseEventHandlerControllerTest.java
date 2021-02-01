package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerControllerTest {

    @Mock
    private InitiationTaskHandler initiationTaskHandler;

    @Test
    void given_evaluateDmn_returns_nothing_then_caseEventHandler_does_not_handle() {
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

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() {
        List<InitiateEvaluateResponse> results = List.of(InitiateEvaluateResponse.builder().build());
        when(initiationTaskHandler.evaluateDmn(any(EventInformation.class))).thenReturn(results);

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
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }
}
