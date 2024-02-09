package uk.gov.hmcts.reform.wacaseeventhandler.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.repository.CaseEventMessageRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CaseEventMessageCacheService {

    @Autowired
    CaseEventMessageRepository caseEventMessageRepository;

    @Cacheable(value = "liveness_database_check_cache", key = "#environment", sync = true,
            cacheManager = "livenessDatabaseCheckCacheManager")
    public List<CaseEventMessageEntity> getAllMessagesInNewState(String environment) {
        log.info("Getting all messages in new state for key {}", environment);
        final List<CaseEventMessageEntity> allMessageInNewState = caseEventMessageRepository.getAllMessagesInNewState();
        final int minNoOfMessages = 1;
        final int noOfNewMessages = allMessageInNewState.size();
        if (noOfNewMessages < minNoOfMessages) {
            log.info("Number of Messages in NEW State {} ", noOfNewMessages);
        } else {
            LocalDateTime oldestMessageTimestamp =
                    allMessageInNewState.get(noOfNewMessages - 1).getEventTimestamp();
            log.info("Number of Messages in NEW State {} with oldest message eventTimeStamp {}",
                    noOfNewMessages, oldestMessageTimestamp);
        }
        return allMessageInNewState;
    }

}
