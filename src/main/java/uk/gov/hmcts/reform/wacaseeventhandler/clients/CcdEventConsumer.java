package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EvaluateResponse;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;
import uk.gov.hmcts.reform.wacaseeventhandler.handlers.CaseEventHandler;

import java.util.List;
import javax.jms.Session;

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
    public void onMessage(String message, @Headers MessageHeaders headers, Session session) {
        try {
            EventInformation eventInformation = objectMapper.readValue(message, EventInformation.class);
            log.debug(String.format("Message received : %s", eventInformation.toString()));
            handleMessage(eventInformation);
        } catch (JsonProcessingException exp) {
            log.error("Unable to parse event", exp);
        }
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    private void handleMessage(EventInformation eventInformation) {
        for (CaseEventHandler handler : handlerServices) {
            List<? extends EvaluateResponse> results = handler.evaluateDmn(eventInformation);
            if (!results.isEmpty()) {
                handler.handle(results, eventInformation);
            }
        }
    }
}
