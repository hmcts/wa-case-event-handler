package uk.gov.hmcts.reform.wacaseeventhandler.clients;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSessionReceiverClient;
import com.microsoft.applicationinsights.TelemetryClient;
import lombok.SneakyThrows;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.MethodSorters;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import uk.gov.hmcts.reform.wacaseeventhandler.config.ServiceBusConfiguration;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.annotation.DirtiesContext.HierarchyMode.CURRENT_LEVEL;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles(profiles = {"db", "integration"})
@ContextConfiguration(classes = EventConsumerIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {"azure.servicebus.enableASB-DLQ=true",
    "azure.servicebus.connection-string="
        + "Endpoint=sb:test;SharedAccessKeyName=test;SharedAccessKey=test;EntityPath=test",
    "azure.servicebus.topic-name=test",
    "azure.servicebus.subscription-name=test",
    "azure.servicebus.ccd-case-events-subscription-name=test",
    "azure.servicebus.retry-duration=2"})
@DirtiesContext(classMode = AFTER_CLASS, hierarchyMode = CURRENT_LEVEL)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EventConsumerIntegrationTest {

    @Captor
    private ArgumentCaptor<ServiceBusReceivedMessage> messageArgumentCaptor;

    @Mock
    private ServiceBusReceivedMessage message;

    @MockBean
    private TelemetryClient telemetryClient;

    private static CaseEventMessageRepository repository;

    private static ServiceBusReceiverClient ccdReceiverClient;

    private static ServiceBusReceiverClient dlqReceiverClient;

    private static final List<ServiceBusReceivedMessage> messageList = new ArrayList<>();

    private static final List<ServiceBusReceivedMessage> dlqMessageList = new ArrayList<>();


    @TestConfiguration
    public static class TestConfig {

        @Bean
        @Primary
        public LaunchDarklyFeatureFlagProvider mockLaunchDarklyFeatureFlagProvider() {
            LaunchDarklyFeatureFlagProvider featureFlagProvider = mock(LaunchDarklyFeatureFlagProvider.class);
            when(featureFlagProvider.getBooleanValue(any(), any())).thenReturn(true);
            return featureFlagProvider;
        }

        @Bean
        @Primary
        public CaseEventMessageRepository mockCaseEventMessageRepository() {
            repository = mock(CaseEventMessageRepository.class);
            return repository;
        }

        @Bean
        @Primary
        public ServiceBusConfiguration mockServiceBusConfiguration() {
            ServiceBusConfiguration serviceBusConfiguration = mock(ServiceBusConfiguration.class);

            ServiceBusSessionReceiverClient ccdCaseEventsSessionReceiver = mock(ServiceBusSessionReceiverClient.class);
            when(serviceBusConfiguration.createCcdCaseEventsSessionReceiver()).thenReturn(ccdCaseEventsSessionReceiver);
            doNothing().when(ccdCaseEventsSessionReceiver).close();
            ccdReceiverClient = mock(ServiceBusReceiverClient.class);
            when(ccdCaseEventsSessionReceiver.acceptNextSession()).thenReturn(ccdReceiverClient);
            doAnswer(invocation -> emitMessage(messageList)).when(ccdReceiverClient).receiveMessages(1);

            dlqReceiverClient = mock(ServiceBusReceiverClient.class);
            when(serviceBusConfiguration.createCcdCaseEventsDeadLetterQueueSessionReceiver())
                .thenReturn(dlqReceiverClient);
            doAnswer(invocation -> emitMessage(dlqMessageList)).when(dlqReceiverClient).receiveMessages(1);

            ServiceBusSessionReceiverClient sessionReceiverClient = mock(ServiceBusSessionReceiverClient.class);
            when(serviceBusConfiguration.createSessionReceiver()).thenReturn(sessionReceiverClient);
            doNothing().when(sessionReceiverClient).close();
            ServiceBusReceiverClient receiverClient = mock(ServiceBusReceiverClient.class);
            when(sessionReceiverClient.acceptNextSession()).thenReturn(receiverClient);
            doAnswer(invocation -> emitMessage(List.of())).when(receiverClient).receiveMessages(1);

            return serviceBusConfiguration;
        }
    }

    @BeforeEach
    public void setUp() {
        Mockito.reset(repository);
        when(message.getBody()).thenReturn(BinaryData.fromString(getCaseEventMessage()));
        when(repository.findByMessageId(any())).thenReturn(List.of());
        when(repository.save(any(CaseEventMessageEntity.class))).thenReturn(null);
    }

    @Test
    public void should_complete_the_message_when_message_processed_successfully() {
        String messageId = "some_message_id_1";
        when(message.getMessageId()).thenReturn(messageId);
        messageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).save(any(CaseEventMessageEntity.class)));

        verify(ccdReceiverClient).complete(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    @Test
    public void should_abandon_the_message_when_database_repository_failure() {
        String messageId = "some_message_id_2";
        when(message.getMessageId()).thenReturn(messageId);
        when(repository.findByMessageId(messageId)).thenThrow(new RuntimeException());
        messageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).findByMessageId(messageId));

        verify(ccdReceiverClient, atLeast(1)).abandon(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    @Test
    public void should_abandon_the_message_when_repository_fail_to_save_the_record() {
        String messageId = "some_message_id_3";
        when(message.getMessageId()).thenReturn(messageId);
        when(repository.save(any(CaseEventMessageEntity.class))).thenThrow(new RuntimeException());
        messageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).save(any(CaseEventMessageEntity.class)));

        verify(ccdReceiverClient, atLeast(1)).abandon(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    @Test
    public void should_complete_the_message_when_dlq_message_processed_successfully() {
        String messageId = "some_message_id_4";
        when(message.getMessageId()).thenReturn(messageId);
        dlqMessageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).save(any(CaseEventMessageEntity.class)));

        verify(dlqReceiverClient).complete(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    @Test
    public void should_abandon_the_message_when_dlq_database_repository_failure() {
        String messageId = "some_message_id_5";
        when(message.getMessageId()).thenReturn(messageId);
        when(repository.findByMessageId(messageId)).thenThrow(new RuntimeException());
        dlqMessageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).findByMessageId(messageId));

        verify(dlqReceiverClient, atLeast(1)).abandon(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    @Test
    public void should_abandon_the_message_when_repository_fail_to_save_dlq_record() {
        String messageId = "some_message_id_6";
        when(message.getMessageId()).thenReturn(messageId);
        when(repository.save(any(CaseEventMessageEntity.class))).thenThrow(new RuntimeException());
        dlqMessageList.add(message);
        await()
            .untilAsserted(() -> verify(repository, times(1)).save(any(CaseEventMessageEntity.class)));

        verify(dlqReceiverClient, atLeast(1)).abandon(messageArgumentCaptor.capture());
        assertEquals(messageId, messageArgumentCaptor.getValue().getMessageId());
    }

    public static String getCaseEventMessage() {
        return "{\n"
            + "  \"EventInstanceId\" : \"some event instance Id\",\n"
            + "  \"EventTimeStamp\" : \"" + LocalDateTime.now() + "\",\n"
            + "  \"CaseId\" : \"caseId\",\n"
            + "  \"JurisdictionId\" : \"ia\",\n"
            + "  \"CaseTypeId\" : \"asylum\",\n"
            + "  \"EventId\" : \"some event Id\",\n"
            + "  \"NewStateId\" : \"some new state Id\",\n"
            + "  \"UserId\" : \"some user Id\",\n"
            + "  \"MessageProperties\" : {\n"
            + "      \"property1\" : \"test1\"\n"
            + "  }\n"
            + "}";
    }

    @SneakyThrows
    private static IterableStream<ServiceBusReceivedMessage> emitMessage(List<ServiceBusReceivedMessage> messages) {
        synchronized (messages) {
            Thread.sleep(2000);
            if (!messages.isEmpty()) {
                final Flux<ServiceBusReceivedMessage> iterableStreamFlux = Flux.<ServiceBusReceivedMessage>create(
                    sink -> {
                        sink.next(messages.remove(0));
                        sink.complete();
                    }).subscribeOn(Schedulers.single());
                return new IterableStream<>(iterableStreamFlux);
            }
            return new IterableStream<>(Flux.empty());
        }
    }
}
