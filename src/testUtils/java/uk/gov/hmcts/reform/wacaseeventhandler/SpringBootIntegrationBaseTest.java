package uk.gov.hmcts.reform.wacaseeventhandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wacaseeventhandler.controllers.CaseEventHandlerController;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningTaskHandler;

@SpringBootTest(classes = {
    Application.class,
    CaseEventHandlerController.class,
    CancellationTaskHandler.class,
    InitiationTaskHandler.class,
    WarningTaskHandler.class
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
public abstract class SpringBootIntegrationBaseTest {

    @Autowired
    protected MockMvc mockMvc;

}
