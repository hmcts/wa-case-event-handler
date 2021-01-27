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

    public boolean processMesssage(String message) {
        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
            log.debug(String.format("Message received : %s", eventInformation.toString()));

            for (CaseEventHandler handler : handlerServices) {
                List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
                if (!results.isEmpty()) {
                    handler.handle(results, eventInformation);
                }
            }
            return true;
        } catch (JsonProcessingException exp) {
            log.error("Unable to parse event", exp);
            throw new RuntimeException(exp);
        }
    }

}
