package uk.gov.hmcts.reform.wacaseeventhandler.services;

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.testing.FakeTicker;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
@ActiveProfiles(profiles = {"db", "integration"})
@TestPropertySource(properties = {"environment=test"})
class CaseEventMessageCacheServiceTest {

    @Autowired
    private CaseEventMessageCacheService caseEventMessageCacheService;

    @Autowired
    private CaseEventMessageRepository caseEventMessageRepository;


    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_return_empty_case_event_messages() {
        TestConfiguration.fakeTicker.advance(1, TimeUnit.HOURS);
        List<CaseEventMessageEntity> messages = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messages).isEmpty();
    }

    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_call_external_api_only_once() {
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
    }


    @Test
    @Sql({"classpath:sql/delete_from_case_event_messages.sql"})
    public void should_change_after_cache_expiry_external_api() {
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

        List<CaseEventMessageEntity> messagesFromCacheAgain = caseEventMessageCacheService.getAllMessagesInNewState("test");
        assertThat(messagesFromCacheAgain).isNotEmpty();

        assertThat(messages).isSameAs(messagesFromCache).isNotSameAs(messagesFromCacheAgain);
        assertEquals(messagesFromDb, messagesFromCacheAgain);
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
