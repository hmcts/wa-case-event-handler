package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
@SuppressWarnings("PMD.DoNotUseThreads")
public class CcdCaseEventsConsumer implements Runnable {

    private final ServiceBusConfiguration serviceBusConfiguration;

    public CcdCaseEventsConsumer(ServiceBusConfiguration serviceBusConfiguration) {
        this.serviceBusConfiguration = serviceBusConfiguration;
    }

    @Override
    @SuppressWarnings("squid:S2189")
    public void run() {
        try (ServiceBusSessionReceiverClient sessionReceiver =
                     serviceBusConfiguration.createCcdCaseEventsSessionReceiver()) {
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
                        try {
                            log.info("Received CCD Case Event message with id '{}'", message.getMessageId());

                            receiver.complete(message);

                            log.info("CCD Case Event message with id '{}' handled successfully",
                                    message.getMessageId());
                        } catch (Exception ex) {
                            log.error("Error processing CCD Case Event message with id '{}' - "
                                    + "will continue to complete message", message.getMessageId());
                            receiver.complete(message);
                        }
                    });
        } catch (IllegalStateException ex) {
            log.info("Timeout: No CCD Case Event messages received waiting for next session.");
        } catch (Exception ex) {
            log.error("Error occurred while closing the session", ex);
        }
    }
}
