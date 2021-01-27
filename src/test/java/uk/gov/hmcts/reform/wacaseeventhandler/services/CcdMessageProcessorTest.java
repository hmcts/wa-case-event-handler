package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wacaseeventhandler.controllers.CaseEventHandlerController;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.initiatetask.InitiateEvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.InitiationTaskHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CcdMessageProcessorTest {

    @Mock
    private InitiationTaskHandler initiationTaskHandler;

    @Mock
    private ObjectMapper objectMapper;

    private EventInformation validEventInformation;

    private CcdMessageProcessor processor;

    @BeforeEach
    void setUp() {
        String fixedDate = "2020-12-07T17:39:22.232622";
        validEventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .dateTime(LocalDateTime.parse(fixedDate))
            .caseReference("some case reference")
            .jurisdictionId("ia")
            .caseTypeId("asylum")
            .eventId("some event Id")
            .newStateId("some new state Id")
            .userId("some user Id")
            .build();
    }

    @Test
    void given_evaluateDmn_returns_something_then_caseEventHandler_does_handle() throws JsonProcessingException {
        List<InitiateEvaluateResponse> results = List.of(InitiateEvaluateResponse.builder().build());
        when(initiationTaskHandler.evaluateDmn(any(EventInformation.class))).thenReturn(results);

        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);
        processor = new CcdMessageProcessor(handlerServices, objectMapper);

        ObjectMapper mapper = new ObjectMapper();
        String jsonEventInformation = mapper.writeValueAsString(validEventInformation);
        when(objectMapper.readValue(jsonEventInformation, EventInformation.class))
            .thenReturn(validEventInformation);

        assertTrue(processor.processMesssage(jsonEventInformation));

        verify(objectMapper, Mockito.times(1))
            .readValue(jsonEventInformation, EventInformation.class);

        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
        verify(initiationTaskHandler).handle(anyList(), any(EventInformation.class));
    }

    @Test
    void given_evaluateDmn_returns_nothing_then_caseEventHandler_does_not_handle() throws JsonProcessingException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        processor = new CcdMessageProcessor(handlerServices, objectMapper);

        ObjectMapper mapper = new ObjectMapper();
        String jsonEventInformation = mapper.writeValueAsString(validEventInformation);
        when(objectMapper.readValue(jsonEventInformation, EventInformation.class))
            .thenReturn(validEventInformation);

        assertTrue(processor.processMesssage(jsonEventInformation));

        verify(objectMapper, Mockito.times(1))
            .readValue(jsonEventInformation, EventInformation.class);
        verify(initiationTaskHandler).evaluateDmn(any(EventInformation.class));
    }


    @Test
    void should_throw_exception_when_serializing_object() throws IOException {
        List<CaseEventHandler> handlerServices = List.of(initiationTaskHandler);

        processor = new CcdMessageProcessor(handlerServices, objectMapper);

        ObjectMapper mapper = new ObjectMapper();
        String jsonEventInformation = mapper.writeValueAsString(validEventInformation);

        when(objectMapper.readValue(jsonEventInformation, EventInformation.class))
            .thenThrow(JsonProcessingException.class);

        Assertions.assertThrows(RuntimeException.class,
                                () -> processor.processMesssage(jsonEventInformation));

        verify(initiationTaskHandler, Mockito.times(0))
            .evaluateDmn(any(EventInformation.class));

    }

}
