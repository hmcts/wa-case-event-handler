package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.hmcts.reform.wacaseeventhandler.clients.CcdEventMessageConsumer;

import javax.annotation.PreDestroy;

@Profile("!local")
@Configuration
public class ServiceBusConfiguration {

    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;
    @Value("${azure.subscription}")
    private String subscription;

    @Autowired
    private CcdEventMessageConsumer consumer;

    private ServiceBusProcessorClient serviceBus;

    @Bean
    public void serviceBusProcessorClient() {
        serviceBus = new ServiceBusClientBuilder()
            .connectionString(host)
            //.retryOptions(new AmqpRetryOptions().setTryTimeout(Duration.ofSeconds(20)))
            .sessionProcessor()
            .topicName(topic)
            .subscriptionName(subscription)
            .processMessage(consumer.consumeMessage())
            .processError(consumer.handleError())
            .buildProcessorClient();

        serviceBus.start();
    }

    @PreDestroy
    public void closeConsumer() {
        serviceBus.close();
    }

}
