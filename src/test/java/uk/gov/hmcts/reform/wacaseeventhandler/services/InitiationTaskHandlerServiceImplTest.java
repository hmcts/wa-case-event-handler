package uk.gov.hmcts.reform.wacaseeventhandler.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WaWorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.DmnStringValue;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnRequest;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerServiceImplTest {

    @Mock
    private WaWorkflowApiClient<InitiateTaskDmnRequest, InitiateTaskDmnResponse> waWorkflowApiClient;
    @Mock
    private AuthTokenGenerator authTokenGenerator;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    @Test
    void can_handle() {
        Mockito.when(authTokenGenerator.generate()).thenReturn("Bearer s2s token");
        Mockito.when(waWorkflowApiClient.evaluateDmn(
            "Bearer s2s token",
            "getTask_IA_Asylum",
            buildInitiateTaskDmnRequest()
        ))
            .thenReturn(Collections.emptyList());

        assertThat(handlerService.canHandle()).isFalse();
    }

    private EvaluateDmnRequest<InitiateTaskDmnRequest> buildInitiateTaskDmnRequest() {
        DmnStringValue eventId = new DmnStringValue("submitAppeal");
        DmnStringValue postEventState = new DmnStringValue("");
        InitiateTaskDmnRequest initiateTaskDmnRequestVariables = new InitiateTaskDmnRequest(eventId, postEventState);

        return new EvaluateDmnRequest<>(initiateTaskDmnRequestVariables);
    }

    @Test
    void handle() throws NoSuchMethodException {
        assertThat(handlerService.getClass().getMethod("handle").getName()).isEqualTo("handle");
    }

}
