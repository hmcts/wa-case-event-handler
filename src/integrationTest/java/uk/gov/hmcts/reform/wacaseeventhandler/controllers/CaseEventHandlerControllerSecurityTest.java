package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.microsoft.applicationinsights.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.filters.ServiceAuthFilter;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CancellationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.ReconfigurationCaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.WarningCaseEventHandler;

import java.util.Collections;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.controllers.CaseEventHandlerControllerEndpointTest.getBaseEventInformation;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(profiles = {"db", "integration"})
class CaseEventHandlerControllerSecurityTest {

    public static final String SOME_SERVICE_AUTHORIZATION = "Bearer some service authorization";
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CancellationCaseEventHandler cancellationCaseEventHandler;
    @MockBean
    private InitiationCaseEventHandler initiationCaseEventHandler;
    @MockBean
    private WarningCaseEventHandler warningCaseEventHandler;
    @MockBean
    private ReconfigurationCaseEventHandler reconfigurationCaseEventHandler;
    @MockBean
    private TelemetryClient telemetryClient;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        mockHandlers();
    }

    @ParameterizedTest
    @MethodSource("scenarioProvider")
    void given_authorised_service_header_should_respond_204(String serviceName, HttpStatus expectedHttpStatus)
        throws Exception {

        when(serviceAuthorisationApi.getServiceName(SOME_SERVICE_AUTHORIZATION)).thenReturn(serviceName);

        mockMvc.perform(post("/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .header(ServiceAuthFilter.AUTHORISATION, SOME_SERVICE_AUTHORIZATION)
                .content(asJsonString(getBaseEventInformation(null))))
            .andExpect(status().is(expectedHttpStatus.value()));
    }

    private void mockHandlers() {
        when(cancellationCaseEventHandler.evaluateDmn(any())).thenReturn(Collections.emptyList());
        when(initiationCaseEventHandler.evaluateDmn(any())).thenReturn(Collections.emptyList());
        when(warningCaseEventHandler.evaluateDmn(any())).thenReturn(Collections.emptyList());
        when(reconfigurationCaseEventHandler.evaluateDmn(any())).thenReturn(Collections.emptyList());
    }

    private static Stream<Arguments> scenarioProvider() {
        Arguments response200ForCaseEventHandlerScenario = Arguments.of("wa_case_event_handler", HttpStatus.NO_CONTENT);
        Arguments response200ForTaskMonitorScenario = Arguments.of("wa_task_monitor", HttpStatus.NO_CONTENT);
        Arguments response403Scenario = Arguments.of("ccd", HttpStatus.FORBIDDEN);

        return Stream.of(response200ForCaseEventHandlerScenario,
            response200ForTaskMonitorScenario,
            response403Scenario);
    }
}

