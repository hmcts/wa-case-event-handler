package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.cancellationtask.CancellationEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handler.warningtask.WarningEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.InitiationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.WarningTaskHandler;

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
    InitiationTaskHandler.class,
    WarningTaskHandler.class
})
class CaseEventHandlerControllerTest {

    public static final String FIXED_DATE = "2020-12-07T17:39:22.232622";
    @MockBean
    private CancellationTaskHandler cancellationTaskHandlerService;

    @MockBean
    private InitiationTaskHandler initiationTaskHandlerService;

    @MockBean
    private WarningTaskHandler warningTaskHandlerService;

    @Autowired
    private CaseEventHandlerController controller;

    @Test
    void given_message_then_apply_handlers_in_order() {

        DmnStringValue action = new DmnStringValue("Cancel");
        DmnStringValue taskCategory = new DmnStringValue("Time extension");
        given(cancellationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(new CancellationEvaluateResponse(action, taskCategory)));

        given(warningTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(new WarningEvaluateResponse()));

        given(initiationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(InitiateEvaluateResponse.builder().build()));

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some id")
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .dateTime(LocalDateTime.parse(FIXED_DATE))
            .caseReference("some case reference")
            .build();

        controller.caseEventHandler(eventInformation);

        InOrder inOrder = inOrder(
            cancellationTaskHandlerService,
            warningTaskHandlerService,
            initiationTaskHandlerService
        );

        inOrder.verify(cancellationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(cancellationTaskHandlerService).handle(anyList(), eq(eventInformation));

        inOrder.verify(warningTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(warningTaskHandlerService).handle(anyList(), eq(eventInformation));

        inOrder.verify(initiationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(initiationTaskHandlerService).handle(anyList(), eq(eventInformation));

    }
}
