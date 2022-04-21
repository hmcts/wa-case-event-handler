package uk.gov.hmcts.reform.wacaseeventhandler.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.CaseEventMessageEntity;
import uk.gov.hmcts.reform.wacaseeventhandler.entity.MessageState;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseEventMessageErrorHandlingRepository extends CrudRepository<CaseEventMessageEntity, Long> {

    String LOCK_AND_GET_MESSAGE_BY_ID_SQL =
        "SELECT * from public.wa_case_event_messages msg where msg.message_id = (:messageId) "
            + "for update skip locked";

    @Query(value = LOCK_AND_GET_MESSAGE_BY_ID_SQL, nativeQuery = true)
    List<CaseEventMessageEntity> findByMessageIdToUpdate(String messageId);

    @Modifying
    @Query(value = CaseEventMessageRepository.UPDATE_CASE_MESSAGE_STATE, nativeQuery = true)
    int updateMessageState(@Param("messageState") MessageState messageState,
                           @Param("messageIds") List<String> messageIds);

    @Modifying
    @Query(value = CaseEventMessageRepository.UPDATE_CASE_MESSAGE_RETRY_DETAILS, nativeQuery = true)
    int updateMessageWithRetryDetails(@Param("retryCount") int retryCount,
                                      @Param("holdUntil") LocalDateTime holdUntil,
                                      @Param("messageId") String messageId);

}
