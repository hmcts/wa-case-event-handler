package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.hmcts.reform.wacaseeventhandler.services.CcdMessageProcessor;

@Component
@Slf4j
public class CcdEventConsumer {

/*    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;
    @Value("${azure.subscription}")
    private String subscription;

    private final ServiceBusSessionReceiverAsyncClient sessionReceiver;

    private final CcdMessageProcessor messageProcessor;

    @Autowired
    public CcdEventConsumer(ServiceBusSessionReceiverAsyncClient sessionReceiver,
                            CcdMessageProcessor messageProcessor) {
        this.sessionReceiver = sessionReceiver;
        this.messageProcessor = messageProcessor;
    }

    @Bean
    public void receiveMessageWithSession() {
        Mono<ServiceBusReceiverAsyncClient> receiverMono = sessionReceiver.acceptSession("sessionId");

        Flux.usingWhen(receiverMono,
                                                 receiver -> receiver.receiveMessages(),
                                                 receiver -> Mono.fromRunnable(() -> receiver.close()))
            .subscribe(message -> {
                // Process message.
                System.out.printf("Session: %s. Sequence #: %s. Contents: %s%n", message.getSessionId(),
                                  message.getSequenceNumber(), message.getBody());

*//*                boolean flag = messageProcessor.processMesssage(new String(message.getBody().toBytes()));
                if(flag) {
                    sessionReceiver.acceptNextSession();
                }*//*

            }, error -> System.err.println("Error occurred: " + error));

    }*/


}
