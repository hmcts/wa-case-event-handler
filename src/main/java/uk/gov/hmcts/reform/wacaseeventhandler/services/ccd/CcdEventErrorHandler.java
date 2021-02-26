package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class CcdEventErrorHandler {

    private final int retryAttempts;
    private final DeadLetterService deadLetterService;

    public CcdEventErrorHandler(DeadLetterService deadLetterService,
                                @Value("${azure.servicebus.retry-attempts}") int retryAttempts) {
        this.deadLetterService = deadLetterService;
        this.retryAttempts = retryAttempts;
    }

    public void handleJsonError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                 String loggerMsg, String incomingMessage, JsonProcessingException exp) {
        log.error("Unable to parse case details = {}", loggerMsg, exp);

        receiver.deadLetter(message, deadLetterService
            .handleParsingError(incomingMessage, exp.getMessage()));

        log.warn("Parsing Error: message = {} sent to dead letter Q", loggerMsg);
    }

    public void handleApplicationError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                        String loggerMsg, String incomingMessage, RestClientException exp) {
        log.error("Unable to process case details = {}", loggerMsg, exp);
        final Long deliveryCount = message.getRawAmqpMessage().getHeader().getDeliveryCount();
        if (deliveryCount >= retryAttempts) {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, exp.getMessage()));
            log.warn("Application Error: Max delivery count reached. "
                         + "Message = {} sent to dead letter Q", loggerMsg);
        } else {
            receiver.abandon(message);
            log.warn("Retrying to process case details = {}", loggerMsg);
        }
    }

    public void handleGenericError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                       String loggerMsg, String incomingMessage, Exception exp) {
        log.error("Unknown error occurred while processing case details = {}", incomingMessage, exp);
        if (StringUtils.isEmpty(exp.getMessage())) {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, "Unknown Error"));
        } else {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, exp.getMessage()));
        }
        log.warn("Unknown Error: message = {} sent to dead letter Q", loggerMsg);
    }

}
