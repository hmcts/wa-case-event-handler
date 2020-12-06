package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.EvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.initiatetask.InitiateTaskEvaluateDmnResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.helpers.InitiateTaskHelper;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @CsvSource({
        "some id, 200",
        ", 400",
        "'', 400",
    })
    void given_message_then_return_expected_status_code(String id, int expectedStatus) throws Exception {
        CcdEventMessage ccdEventMessage = CcdEventMessage.builder()
            .id(id)
            .name("some name")
            .build();

        mockMvc.perform(post("/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(ccdEventMessage)))
            .andDo(print())
            .andExpect(status().is(expectedStatus));
    }

    private String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

}

