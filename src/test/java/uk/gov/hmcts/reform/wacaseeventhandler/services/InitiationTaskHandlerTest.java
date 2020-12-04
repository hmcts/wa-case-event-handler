package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.Builder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WorkflowApiClientToInitiateTask;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;
import uk.gov.hmcts.reform.wacaseeventhandler.services.initiatetask.InitiationTaskHandler;

import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InitiationTaskHandlerTest {

    @Mock
    private WorkflowApiClientToInitiateTask apiClientToInitiateTask;

    @InjectMocks
    private InitiationTaskHandler handlerService;

    @ParameterizedTest
    @MethodSource(value = "scenarioProvider")
    void can_handle(Scenario scenario) {

        Mockito.when(apiClientToInitiateTask.evaluateDmn(
            "getTask_IA_Asylum",
            InitiateTaskHelper.buildInitiateTaskDmnRequest()
        ))
            .thenReturn(scenario.evaluateDmnResponses);

        assertThat(handlerService.canHandle()).isEqualTo(scenario.expected);
    }

    private static Stream<Scenario> scenarioProvider() {
        Scenario cannotHandledScenario = Scenario.builder()
            .evaluateDmnResponses(new EvaluateDmnResponse<>(Collections.emptyList()))
            .expected(false)
            .build();

        Scenario canHandledScenario = Scenario.builder()
            .evaluateDmnResponses(InitiateTaskHelper.buildInitiateTaskDmnResponse())
            .expected(true)
            .build();

        return Stream.of(cannotHandledScenario, canHandledScenario);
    }

    @Builder
    private static class Scenario {
        EvaluateDmnResponse<InitiateTaskDmnResponse> evaluateDmnResponses;
        boolean expected;
    }

    @Test
    void handle() throws NoSuchMethodException {
        assertThat(handlerService.getClass().getMethod("handle").getName()).isEqualTo("handle");
    }
}
