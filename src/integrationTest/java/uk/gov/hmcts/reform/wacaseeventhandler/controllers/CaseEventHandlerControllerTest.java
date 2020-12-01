package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.CcdEventMessage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"local"})
@WebMvcTest(value = {CaseEventHandlerController.class})
class CaseEventHandlerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void given_valid_message_then_return_200() throws Exception {
        CcdEventMessage ccdEventMessage = CcdEventMessage.builder()
            .id("some id")
            .name("some name")
            .build();

        mockMvc.perform(post("/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(ccdEventMessage)))
            .andDo(print())
            .andExpect(status().isOk());
    }

    private String asJsonString(final Object obj) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(obj);
    }

    @Test
    void given_invalid_message_then_return_400() throws Exception {
        CcdEventMessage ccdEventMessage = CcdEventMessage.builder()
            .name("some name")
            .build();

        mockMvc.perform(post("/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(ccdEventMessage)))
            .andDo(print())
            .andExpect(status().isBadRequest());

    }
}

