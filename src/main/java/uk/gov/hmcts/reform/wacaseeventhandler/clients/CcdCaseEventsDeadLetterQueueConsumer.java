package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@Profile("!functional & !local")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdCaseEventsDeadLetterQueueConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;
    private final EventMessageReceiverService eventMessageReceiverService;

    public CcdCaseEventsDeadLetterQueueConsumer(ServiceBusConfiguration serviceBusConfiguration,
                                                EventMessageReceiverService eventMessageReceiverService) {
        this.serviceBusConfiguration = serviceBusConfiguration;
        this.eventMessageReceiverService = eventMessageReceiverService;
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        try (ServiceBusReceiverClient sessionReceiver =
                     serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver()) {
            while (true) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusReceiverClient receiver) {
        try {
            receiver.receiveMessages(1).forEach(
                message -> {
                    final String messageId = message.getMessageId();
                    try {
                        log.info("Received CCD Case Event Dead Letter Queue message with id '{}'", messageId);

                        eventMessageReceiverService.handleDlqMessage(
                            messageId,
                            message.getSessionId(),
                            new String(message.getBody().toBytes())
                        );

                        receiver.complete(message);

                        log.info(
                            "CCD Case Event Dead Letter Queue message with id '{}' handled successfully",
                            messageId
                        );
                    } catch (Exception ex) {
                        log.error("Error processing CCD Case Event Dead Letter Queue message with id '{}' - "
                                      + "abandon the processing and ASB will re-deliver it", messageId);
                        receiver.abandon(message);
                    }
                });
        } catch (Exception ex) {
            log.error("Error occurred while completing the message processing", ex);
        }
    }
}
