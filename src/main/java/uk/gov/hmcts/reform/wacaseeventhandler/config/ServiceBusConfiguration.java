package uk.gov.hmcts.reform.wacaseeventhandler.config;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!local")
@Configuration
public class ServiceBusConfiguration {

    @Value("${azure.host}")
    private String host;
    @Value("${azure.topic}")
    private String topic;
    @Value("${azure.subscription}")
    private String subscription;

    @Bean
    public ServiceBusSessionReceiverAsyncClient serviceBusSessionReceiverConfig() {

        ServiceBusSessionReceiverAsyncClient sessionReceiver = new ServiceBusClientBuilder()
            .connectionString(host)
            .sessionReceiver()
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .topicName(topic)
            .subscriptionName(subscription)
            .buildAsyncClient();

        return sessionReceiver;
    }

/*    @Bean
    public ServiceBusSenderClient createConnection() {
        ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(host)
            .sender()
            .topicName(topic)
            .buildClient();

        return sender;
    }*/

/*    @Bean
    public ServiceBusProcessorClient serviceBusProcessorClient() {
        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
            .connectionString(host)
            .processor()
            .topicName(topic)
            .subscriptionName(subscription)
            //.processMessage(new TopicConsumer())
            //.processError(processError)
            .buildProcessorClient();

        return processorClient;
    }*/

/*    @Bean
    public void startProcessor(ServiceBusProcessorClient serviceBusProcessorClient) {
        serviceBusProcessorClient.start();
    }

    @Bean
    public void stopProcessor(ServiceBusProcessorClient serviceBusProcessorClient) {
        serviceBusProcessorClient.stop();
    }*/

}
