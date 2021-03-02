package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@Scope("prototype")
@ConditionalOnProperty("azure.servicebus.enableASB")
public class ServiceBusConfiguration {

    @Value("${azure.servicebus.connection-string}")
    private String connectionString;
    @Value("${azure.servicebus.topic-name}")
    private String topicName;
    @Value("${azure.servicebus.subscription-name}")
    private String subscriptionName;
    @Value("${azure.servicebus.retry-duration}")
    private int retryTime;

    public ServiceBusSessionReceiverClient createSessionReceiver() {
        log.info("Creating Session receiver");
        ServiceBusSessionReceiverClient client = new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .retryOptions(retryOptions())
            .sessionReceiver()
            .topicName(topicName)
            .subscriptionName(subscriptionName)
            .buildClient();

        log.info("Session receiver created, successfully");
        return client;
    }

    private AmqpRetryOptions retryOptions() {
        AmqpRetryOptions retryOptions = new AmqpRetryOptions();
        retryOptions.setTryTimeout(Duration.ofSeconds(Integer.valueOf(retryTime)));

        return retryOptions;
    }

}
