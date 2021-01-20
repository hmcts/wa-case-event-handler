package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.List;

@Component
@Slf4j
public class CcdEventConsumer {

    private final List<CaseEventHandler> handlerServices;
    private final ObjectMapper objectMapper;

    @Autowired
    public CcdEventConsumer(List<CaseEventHandler> handlerServices, ObjectMapper objectMapper) {
        this.handlerServices = handlerServices;
        this.objectMapper = objectMapper;
    }

    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "jmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )
    public void onMessage(String message) {

        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
            log.debug(String.format("Message received : %s", eventInformation.toString()));
            if(!"ia".equals(eventInformation.getJurisdictionId())) {
                throw new RuntimeException();
            }
        } catch (JsonProcessingException exp) {
            log.error("Unable to parse event", exp);
        }
    }


    private void handleMessage(EventInformation eventInformation) {
        for (CaseEventHandler handler : handlerServices) {
            List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
            if (!results.isEmpty()) {
                handler.handle(results, eventInformation);
            }
        }
    }
}
