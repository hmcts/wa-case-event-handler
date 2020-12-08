package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.cancellationtask.CancellationTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.warningtask.WarningTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.WarningTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask.InitiationTaskHandler;

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

    @MockBean
    private CancellationTaskHandler cancellationTaskHandlerService;

    @MockBean
    private InitiationTaskHandler initiationTaskHandlerService;

    @MockBean
    private WarningTaskHandler warningTaskHandlerService;

    @Autowired
    private CaseEventHandlerController controller;

    @SuppressWarnings("unchecked")
    @Test
    void given_message_then_apply_handlers_in_order() {

        given(cancellationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(new CancellationTaskEvaluateDmnResponse()));

        given(warningTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(new WarningTaskEvaluateDmnResponse()));

        given(initiationTaskHandlerService.evaluateDmn(any(EventInformation.class)))
            .willReturn(List.of(InitiateTaskEvaluateDmnResponse.builder().build()));

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some id")
            .caseTypeId("some case type")
            .jurisdictionId("some jurisdiction")
            .dueTime(LocalDateTime.now())
            .build();

        controller.caseEventHandler(eventInformation);

        InOrder inOrder = inOrder(
            cancellationTaskHandlerService,
            warningTaskHandlerService,
            initiationTaskHandlerService
        );

        inOrder.verify(cancellationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(cancellationTaskHandlerService).handle(
            anyList(),
            eq("some case type"),
            eq("some jurisdiction")
        );

        inOrder.verify(warningTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(warningTaskHandlerService).handle(
            anyList(),
            eq("some case type"),
            eq("some jurisdiction")
        );

        inOrder.verify(initiationTaskHandlerService).evaluateDmn(any(EventInformation.class));
        inOrder.verify(initiationTaskHandlerService).handle(
            anyList(),
            eq("some case type"),
            eq("some jurisdiction")
        );

    }
}
