package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CancellationTaskHandlerServiceImpl;
import uk.gov.hmcts.reform.wacaseeventhandler.services.InitiationTaskHandlerServiceImpl;
import uk.gov.hmcts.reform.wacaseeventhandler.services.WarningHandlerServiceImpl;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

@SpringBootTest
class CaseEventHandlerControllerTest {

    @MockBean
    private CancellationTaskHandlerServiceImpl cancellationTaskHandlerService;

    @MockBean
    private InitiationTaskHandlerServiceImpl initiationTaskHandlerService;

    @MockBean
    private WarningHandlerServiceImpl warningHandlerService;

    @Autowired
    private CaseEventHandlerController controller;

    @Test
    void given_message_then_apply_handlers_in_order() {

        given(cancellationTaskHandlerService.canHandle()).willReturn(true);
        given(warningHandlerService.canHandle()).willReturn(true);
        given(initiationTaskHandlerService.canHandle()).willReturn(true);

        CcdEventMessage ccdEventMessage = CcdEventMessage.builder()
            .id("some id")
            .name("some name")
            .build();

        controller.caseEventHandler(ccdEventMessage);

        InOrder inOrder = inOrder(
            cancellationTaskHandlerService,
            warningHandlerService,
            initiationTaskHandlerService
        );

        inOrder.verify(cancellationTaskHandlerService).canHandle();
        inOrder.verify(cancellationTaskHandlerService).handle();

        inOrder.verify(warningHandlerService).canHandle();
        inOrder.verify(warningHandlerService).handle();

        inOrder.verify(initiationTaskHandlerService).canHandle();
        inOrder.verify(initiationTaskHandlerService).handle();

    }
}
