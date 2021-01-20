package uk.gov.hmcts.reform.wacaseeventhandler.servicebus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.wacaseeventhandler.domain.handlers.common.EventInformation;

import javax.jms.*;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = {"local"})
public class CcdEventPublisherTest {

    @Value("${amqp.topic}")
    private String destination;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void sendMessage() throws JsonProcessingException, JMSException {
        EventInformation eventInformation = buildMessage();
        String eventInfo = objectMapper.writeValueAsString(eventInformation);

        jmsTemplate.convertAndSend(destination, eventInfo, message -> {
            message.setJMSCorrelationID(eventInformation.getCaseId());
            return message;
        });

        waitSeconds(1);
    }

    private EventInformation buildMessage() {
        EventInformation eventInformation = EventInformation.builder()
            .eventInstanceId("some event instance Id")
            .eventTimeStamp(ZonedDateTime.now().plusDays(2).toLocalDateTime())
            .caseId(UUID.randomUUID().toString())
            .jurisdictionId("IA")
            .caseTypeId("Asylum")
            .eventId("requestRespondentEvidence")
            .newStateId("awaitingRespondentEvidence")
            .previousStateId("")
            .userId("some user Id")
            .build();

        return eventInformation;
    }

    private void waitSeconds(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
