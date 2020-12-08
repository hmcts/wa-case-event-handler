package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper.asJsonString;

@ActiveProfiles({"local"})
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CaseEventHandlerControllerEndPointTest {

    public static final String S2S_TOKEN = "Bearer s2s token";

    @MockBean
    private AuthTokenGenerator authTokenGenerator;

    @MockBean
    private RestTemplate restTemplate;

    @Value("${wa-workflow-api.url}")
    private String workflowApiUrl;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        Mockito.when(authTokenGenerator.generate()).thenReturn(S2S_TOKEN);

        mockRestTemplate();
    }

    private void mockRestTemplate() {
        ResponseEntity<EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse>> responseEntity =
            new ResponseEntity<>(
                InitiateTaskHelper.buildInitiateTaskDmnResponse(),
                HttpStatus.OK
            );

        String url = String.format(
            "%s/workflow/decision-definition/key/%s/evaluate",
            workflowApiUrl,
            "getTask_IA_Asylum"
        );
        Mockito.when(restTemplate.exchange(
            ArgumentMatchers.eq(url),
            ArgumentMatchers.eq(HttpMethod.POST),
            ArgumentMatchers.<HttpEntity<List<HttpHeaders>>>any(),
            ArgumentMatchers.<ParameterizedTypeReference<EvaluateDmnResponse<InitiateTaskEvaluateDmnResponse>>>any())
        ).thenReturn(responseEntity);
    }

    @ParameterizedTest
    @MethodSource(value = "scenarioProvider")
    void given_message_then_return_expected_status_code(Scenario scenario) throws Exception {

        mockMvc.perform(post("/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(scenario.eventInformation)))
            .andDo(print())
            .andExpect(status().is(scenario.expectedStatus));
    }

    private static Stream<Scenario> scenarioProvider() {
        EventInformation validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.now())
            .caseReference("some case reference")
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario validEventInformationScenario200 = Scenario.builder()
            .eventInformation(validEventInformation)
            .expectedStatus(HttpStatus.NO_CONTENT.value())
            .build();

        EventInformation invalidEventInformationBecauseMandatoryFieldCannotBeNull = EventInformation.builder()
            .eventInstanceId(null)
            .caseReference("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario mandatoryFieldCannotBeNullScenario400 = Scenario.builder()
            .eventInformation(invalidEventInformationBecauseMandatoryFieldCannotBeNull)
            .expectedStatus(HttpStatus.BAD_REQUEST.value())
            .build();

        EventInformation invalidEventInformationBecauseMandatoryFieldCannotBeEmpty = EventInformation.builder()
            .eventInstanceId("")
            .caseReference("some case reference")
            .jurisdictionId("somme jurisdiction Id")
            .caseTypeId("some case type Id")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();

        Scenario mandatoryFieldCannotBeEmptyScenario400 = Scenario.builder()
            .eventInformation(invalidEventInformationBecauseMandatoryFieldCannotBeEmpty)
            .expectedStatus(HttpStatus.BAD_REQUEST.value())
            .build();


        return Stream.of(
            validEventInformationScenario200,
            mandatoryFieldCannotBeNullScenario400,
            mandatoryFieldCannotBeEmptyScenario400
        );
    }

    @Builder
    private static class Scenario {
        EventInformation eventInformation;
        int expectedStatus;
    }

}

