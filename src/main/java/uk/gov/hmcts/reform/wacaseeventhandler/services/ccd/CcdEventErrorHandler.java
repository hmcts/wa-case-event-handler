package uk.gov.hmcts.reform.wacaseeventhandler.services.ccd;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
                                 String loggerMsg, String incomingMessage, Throwable exp) {
        log.error(String.format("Unable to parse incoming message: %s on case details: %s",
                                incomingMessage, loggerMsg
        ), exp);

        receiver.deadLetter(message, deadLetterService
            .handleParsingError(incomingMessage, exp.getMessage()));

        log.warn(String.format("Dead lettering: %s", loggerMsg));
    }

    public void handleApplicationError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                        String loggerMsg, String incomingMessage, Throwable exp) {
        log.error(String.format("Unable to process case details: %s", loggerMsg), exp);
        final Long deliveryCount = message.getRawAmqpMessage().getHeader().getDeliveryCount();
        if (deliveryCount >= retryAttempts) {
            log.warn(String.format("Max delivery count reached. Dead Lettering: %s", loggerMsg));
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, exp.getMessage()));
            log.warn(String.format("Dead lettering: %s", loggerMsg));
        } else {
            receiver.abandon(message);
            log.warn(String.format("Retrying to process case details: %s", loggerMsg));
        }
    }

    public void handleGenericError(ServiceBusReceiverClient receiver, ServiceBusReceivedMessage message,
                                       String loggerMsg, String incomingMessage, Throwable exp) {
        log.error(String.format("Unknown error occurred while process case details: %s", loggerMsg), exp);
        if (StringUtils.isEmpty(exp.getMessage())) {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, "Unknown Error"));
        } else {
            receiver.deadLetter(message, deadLetterService
                .handleApplicationError(incomingMessage, exp.getMessage()));
        }
        log.warn(String.format("Dead lettering: %s", loggerMsg));
    }

}
