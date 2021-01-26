package uk.gov.hmcts.reform.wacaseeventhandler.controllers;

import com.azure.core.amqp.models.AmqpAnnotatedMessage;
import com.azure.core.amqp.models.AmqpMessageBody;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.spring.integration.SpringIntegrationSerenityRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.SpringBootFunctionalBaseTest;

@Slf4j
public class TestPublisher extends SpringBootFunctionalBaseTest {

    @Autowired
    private ServiceBusSenderClient sender;

    @Test
    public void sendMessage1() throws InterruptedException {
        ServiceBusMessage message = new ServiceBusMessage("This message is with session ::::: event");
        message.setSessionId("event");

        sender.sendMessage(message);

        Thread.sleep(5000);

        message = new ServiceBusMessage("This message is with session :::::: event1");
        message.setSessionId("event1");
        sender.sendMessage(message);

        Thread.sleep(5000);

        message = new ServiceBusMessage("This message is with session :::::: event");
        message.setSessionId("event");
        sender.sendMessage(message);

        Thread.sleep(5000);
        message = new ServiceBusMessage("This message is with session :::::: event2");
        message.setSessionId("event2");
        sender.sendMessage(message);

        Thread.sleep(5000);
        sender.close();
    }
}
