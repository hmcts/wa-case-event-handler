package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.response.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.ccd.message.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CancellationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningCaseEventHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static uk.gov.hmcts.reform.wacaseeventhandler.domain.camunda.DmnValue.dmnStringValue;

@SpringBootTest(classes = {
    CaseEventHandlerController.class,
    CancellationCaseEventHandler.class,
    InitiationCaseEventHandler.class,
    WarningCaseEventHandler.class
})
class CaseEventHandlerControllerTest {

    public static final String FIXED_DATE = "2020-12-07T17:39:22.232622";
    @MockBean
    private CancellationCaseEventHandler cancellationTaskHandlerService;

    @MockBean
    private InitiationCaseEventHandler initiationTaskHandlerService;

    @MockBean
    private WarningCaseEventHandler warningTaskHandlerService;

    @Autowired
    private CaseEventHandlerController controller;

    @Test
    void given_message_then_apply_handlers_in_order() {

        DmnValue<String> cancelAction = dmnStringValue("Cancel");
        DmnValue<String> warnAction = dmnStringValue("Warn");
        DmnValue<String> taskCategory = dmnStringValue("Time extension");

        EvaluateDmnResponse<CancellationEvaluateResponse> cancellationDmnResponse =
            new EvaluateDmnResponse<>(List.of(new CancellationEvaluateResponse(cancelAction, taskCategory, null)));

        doReturn(cancellationDmnResponse.getResults())
            .when(cancellationTaskHandlerService).evaluateDmn(any(EventInformation.class));

        EvaluateDmnResponse<CancellationEvaluateResponse> warningDmnResponse =
            new EvaluateDmnResponse<>(List.of(new CancellationEvaluateResponse(warnAction, taskCategory, null)));

        doReturn(warningDmnResponse.getResults())
            .when(warningTaskHandlerService).evaluateDmn(any(EventInformation.class));

        EvaluateDmnResponse<InitiateEvaluateResponse> initiationDmnResponse =
            new EvaluateDmnResponse<>(List.of(InitiateEvaluateResponse.builder().build()));

        doReturn(initiationDmnResponse.getResults())
            .when(initiationTaskHandlerService).evaluateDmn(any(EventInformation.class));


        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some id")
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .eventTimeStamp(LocalDateTime.parse(FIXED_DATE))
            .caseId("some case reference")
            .build();

        controller.caseEventHandler(eventInformation);

        InOrder inOrder = inOrder(
            cancellationTaskHandlerService,
            initiationTaskHandlerService,
            warningTaskHandlerService
        );


        inOrder.verify(cancellationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(cancellationTaskHandlerService).handle(anyList(), eq(eventInformation));

        inOrder.verify(warningTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(warningTaskHandlerService).handle(anyList(), eq(eventInformation));

        inOrder.verify(initiationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(initiationTaskHandlerService).handle(anyList(), eq(eventInformation));

    }
}
