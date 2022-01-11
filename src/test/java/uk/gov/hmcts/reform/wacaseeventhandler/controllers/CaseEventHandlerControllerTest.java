package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CaseEventHandlerControllerTest {

    @Mock
    private InitiationCaseEventHandler initiationTaskHandler;

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
        EvaluateDmnResponse<InitiateEvaluateResponse> dmnResponse =
            new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(dmnResponse.getResults()).when(initiationTaskHandler).evaluateDmn(any(EventInformation.class));

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
