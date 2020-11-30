package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
        mockMvc.perform(post("/messages")
                            .content("valid message"))
            .andDo(print())
            .andExpect(status().isOk());
    }

    @Test
    void given_invalid_message_then_return_422() throws Exception {
        mockMvc.perform(post("/messages")
                            .content("valid msg"))
            .andDo(print())
            .andExpect(status().isUnprocessableEntity());

    }
}

