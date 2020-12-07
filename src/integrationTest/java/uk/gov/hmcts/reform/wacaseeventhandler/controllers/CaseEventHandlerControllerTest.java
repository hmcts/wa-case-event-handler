package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.WarningTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask.InitiationTaskHandler;

import java.time.LocalDateTime;

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

    @Test
    void given_message_then_apply_handlers_in_order() {

        given(cancellationTaskHandlerService.canHandle()).willReturn(true);
        given(warningTaskHandlerService.canHandle()).willReturn(true);
        given(initiationTaskHandlerService.canHandle()).willReturn(true);

        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some id")
            .dueTime(LocalDateTime.now())
            .build();

        controller.caseEventHandler(eventInformation);

        InOrder inOrder = inOrder(
            cancellationTaskHandlerService,
            warningTaskHandlerService,
            initiationTaskHandlerService
        );

        inOrder.verify(cancellationTaskHandlerService).canHandle();
        inOrder.verify(cancellationTaskHandlerService).handle();

        inOrder.verify(warningTaskHandlerService).canHandle();
        inOrder.verify(warningTaskHandlerService).handle();

        inOrder.verify(initiationTaskHandlerService).canHandle();
        inOrder.verify(initiationTaskHandlerService).handle();

    }
}
