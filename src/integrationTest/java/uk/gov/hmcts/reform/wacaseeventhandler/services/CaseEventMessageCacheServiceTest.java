package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.wacaseeventhandler.Application;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"environment=test"})
class CaseEventMessageCacheServiceTest {

    @Autowired
    private CaseEventMessageCacheService caseEventMessageCacheService;

    @SpyBean
    private CaseEventMessageRepository caseEventMessageRepository;

    @BeforeEach
    void setup() {
        reset(caseEventMessageRepository);
    }

    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_return_empty_case_event_messages() {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        List<CaseEventMessageEntity> messages = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messages).isEmpty();
    }

    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_serve_service_call_from_cache() {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        List<CaseEventMessageEntity> messages = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messages).isEmpty();

        CaseEventMessageEntity caseEventMessage = new CaseEventMessageEntity();
        caseEventMessage.setSequence(1001L);
        caseEventMessage.setCaseId("1234");
        caseEventMessage.setMessageId("ABCD");
        caseEventMessage.setState(MessageState.NEW);
        caseEventMessage.setReceived(LocalDateTime.now().minusMinutes(45));
        caseEventMessage.setEventTimestamp(LocalDateTime.now().minusMinutes(50));
        caseEventMessage.setFromDlq(false);
        caseEventMessage.setDeliveryCount(1);
        caseEventMessage.setRetryCount(1);
        caseEventMessageRepository.save(caseEventMessage);

        List<CaseEventMessageEntity> messagesFromDb = IterableUtils.toList(caseEventMessageRepository.findAll());

        assertThat(messagesFromDb.size()).isEqualTo(1);

        List<CaseEventMessageEntity> messagesFromCache = caseEventMessageCacheService.getAllMessagesInNewState("test");

        assertThat(messages).isSameAs(messagesFromCache);
        assertNotEquals(messages, messagesFromDb);
        verify(caseEventMessageRepository, times(1)).getAllMessagesInNewState();
        verify(caseEventMessageRepository, times(1)).findAll();
    }


    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_invalidate_cache_and_serve_service_call_from_database_call() {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        List<CaseEventMessageEntity> messages = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messages).isEmpty();

        CaseEventMessageEntity caseEventMessage = new CaseEventMessageEntity();
        caseEventMessage.setSequence(1001L);
        caseEventMessage.setCaseId("1234");
        caseEventMessage.setMessageId("ABCD");
        caseEventMessage.setState(MessageState.NEW);
        caseEventMessage.setReceived(LocalDateTime.now().minusMinutes(45));
        caseEventMessage.setEventTimestamp(LocalDateTime.now().minusMinutes(50));
        caseEventMessage.setFromDlq(false);
        caseEventMessage.setDeliveryCount(1);
        caseEventMessage.setRetryCount(1);
        caseEventMessageRepository.save(caseEventMessage);
        List<CaseEventMessageEntity> messagesFromDb = IterableUtils.toList(caseEventMessageRepository.findAll());
        assertThat(messagesFromDb.size()).isEqualTo(1);

        TestConfiguration.fakeTicker.advance(10, TimeUnit.MINUTES);

        List<CaseEventMessageEntity> messagesFromCache = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messagesFromCache).isEmpty();

        assertThat(messages).isSameAs(messagesFromCache);
        assertNotEquals(messages, messagesFromDb);

        TestConfiguration.fakeTicker.advance(25, TimeUnit.MINUTES);

        List<CaseEventMessageEntity> messagesFromCacheAgain =
            caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messagesFromCacheAgain).isNotEmpty();

        assertThat(messages).isSameAs(messagesFromCache).isNotSameAs(messagesFromCacheAgain);
        assertEquals(messagesFromDb, messagesFromCacheAgain);
        verify(caseEventMessageRepository, times(2)).getAllMessagesInNewState();
        verify(caseEventMessageRepository, times(1)).findAll();
    }

    @Configuration
    @Import(Application.class)
    public static class TestConfiguration {

        static FakeTicker fakeTicker = new FakeTicker();

        @Bean
        public Ticker ticker() {
            return fakeTicker::read;
        }

    }
}
