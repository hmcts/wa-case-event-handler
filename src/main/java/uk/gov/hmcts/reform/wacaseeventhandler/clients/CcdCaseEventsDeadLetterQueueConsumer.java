package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.services.EventMessageReceiverService;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
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
        try (ServiceBusSessionReceiverClient sessionReceiver =
                     serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver()) {
            while (true) {
                consumeMessage(sessionReceiver);
            }
        }
    }

    @SuppressWarnings({"PMD.DataflowAnomalyAnalysis"})
    protected void consumeMessage(ServiceBusSessionReceiverClient sessionReceiver) {
        try (ServiceBusReceiverClient receiver = sessionReceiver.acceptNextSession()) {
            receiver.receiveMessages(1)
                .forEach(
                    message -> {
                        final String messageId = message.getMessageId();
                        try {
                            log.info("Received CCD Case Event Dead Letter Queue message with id '{}'", messageId);

                            eventMessageReceiverService.handleDlqMessage(messageId,
                                    new String(message.getBody().toBytes()));

                            receiver.complete(message);

                            log.info("CCD Case Event Dead Letter Queue message with id '{}' handled successfully",
                                    messageId);
                        } catch (Exception ex) {
                            log.error("Error processing CCD Case Event Dead Letter Queue message with id '{}' - "
                                    + "will continue to complete message", messageId);
                            receiver.complete(message);
                        }
                    });
        } catch (IllegalStateException ex) {
            log.info("Timeout: No CCD Case Event Dead Letter Queue messages received waiting for next session.");
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }
}
