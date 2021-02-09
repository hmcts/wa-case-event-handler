package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty("azure.enableASB")
public class ServiceBusConfiguration {

    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;
    @Value("${azure.subscription}")
    private String subscription;
    @Value("${azure.numberOfConcurrentSessions}")
    private String numberOfConcurrentSessions;

    @Autowired
    private CcdEventMessageConsumer consumer;

    private ServiceBusProcessorClient serviceBusClient;

    @Bean
    public void serviceBusProcessorClient() {
        serviceBusClient = new ServiceBusClientBuilder()
            .connectionString(host)
            //.retryOptions(new AmqpRetryOptions().setTryTimeout(Duration.ofSeconds(20)))
            .sessionProcessor()
            .topicName(topic)
            .subscriptionName(subscription)
            .maxConcurrentSessions(Integer.valueOf(numberOfConcurrentSessions))
            .processMessage(consumer.consumeMessageFromChannel())
            .processError(consumer.handleError())
            .buildProcessorClient();

        serviceBusClient.start();
    }

    @PreDestroy
    public void close() {
        if (serviceBusClient != null) {
            serviceBusClient.close();
        }
    }

}
