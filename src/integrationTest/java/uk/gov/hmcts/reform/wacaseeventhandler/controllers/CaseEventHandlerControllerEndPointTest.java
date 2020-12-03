package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.WaWorkflowApiClient;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CancellationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.InitiationTaskHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.services.WarningTaskHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"local"})
@WebMvcTest(value = {
    CaseEventHandlerController.class,
    CancellationTaskHandler.class,
    InitiationTaskHandler.class,
    WarningTaskHandler.class,
    WaWorkflowApiClient.class,
    AuthTokenGenerator.class
})
class CaseEventHandlerControllerEndPointTest {

    @Autowired
    private MockMvc mockMvc;

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

