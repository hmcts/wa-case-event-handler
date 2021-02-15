package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
//import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.warningtask.WarningResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;
//import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningTaskHandler;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@SpringBootTest(classes = {
    CaseEventHandlerController.class,
    CancellationTaskHandler.class,
    InitiationTaskHandler.class
    //WarningTaskHandler.class
})
class CaseEventHandlerControllerTest {

    public static final String FIXED_DATE = "2020-12-07T17:39:22.232622";
    @MockBean
    private CancellationTaskHandler cancellationTaskHandlerService;

    @MockBean
    private InitiationTaskHandler initiationTaskHandlerService;

    //@MockBean
    //private WarningTaskHandler warningTaskHandlerService;

    @Autowired
    private CaseEventHandlerController controller;

    @Test
    void given_message_then_apply_handlers_in_order() {

        DmnStringValue cancelAction = new DmnStringValue("Cancel");
        DmnStringValue warnAction = new DmnStringValue("Warn");
        DmnStringValue taskCategory = new DmnStringValue("Time extension");
        given(cancellationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(new CancellationEvaluateResponse(cancelAction, taskCategory)));

        //given(warningTaskHandlerService.evaluateDmn(any(EventInformation.class)))
        //    .willReturn(List.of(new WarningResponse(warnAction,taskCategory)));

        given(initiationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(InitiateEvaluateResponse.builder().build()));

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
            //warningTaskHandlerService,
            initiationTaskHandlerService
        );

        inOrder.verify(cancellationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(cancellationTaskHandlerService).handle(anyList(), eq(eventInformation));
        //
        //inOrder.verify(warningTaskHandlerService).evaluateDmn(any(EventInformation.class));
        //inOrder.verify(warningTaskHandlerService).handle(anyList(), eq(eventInformation));

        inOrder.verify(initiationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(initiationTaskHandlerService).handle(anyList(), eq(eventInformation));

    }
}
