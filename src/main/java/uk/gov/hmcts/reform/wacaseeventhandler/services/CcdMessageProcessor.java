package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.List;

@Slf4j
@Service
public class CcdMessageProcessor {

    private final List<CaseEventHandler> handlerServices;
    private final ObjectMapper objectMapper;

    public CcdMessageProcessor(List<CaseEventHandler> handlerServices, ObjectMapper objectMapper) {
        this.handlerServices = handlerServices;
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public void processMesssage(String message) throws JsonProcessingException {
        EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
        log.info(String.format("Message received from topic: %s", eventInformation.toString()));

        for (CaseEventHandler handler : handlerServices) {
            List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
            if (!results.isEmpty()) {
                handler.handle(results, eventInformation);
            }
        }
    }

}
