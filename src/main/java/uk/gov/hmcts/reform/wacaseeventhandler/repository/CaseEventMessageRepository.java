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
public interface CaseEventMessageRepository extends CrudRepository<CaseEventMessageEntity, Long> {

    String LOCK_AND_GET_NEXT_MESSAGE_SQL =
            "select * "
                    + "from public.wa_case_event_messages msg "
                    + "where msg.state = 'READY' "
                    + "and (msg.case_id, msg.event_timestamp) in ( "
                    + "  select case_id, min(event_timestamp) "
                    + "  from wa_case_event_messages "
                    + "  where state != 'PROCESSED' "
                    + "  group by case_id) "
                    + "and not exists (select 1 from wa_case_event_messages e "
                    + "                where e.case_id = msg.case_id "
                    + "                and e.event_timestamp is null) "
                    + "and not exists (select 1 from wa_case_event_messages c "
                    + "                where c.case_id is null) "
                    + "and ( "
                    + "  not msg.from_dlq "
                    + "  or ( "
                    + "    msg.from_dlq and ( "
                    + "      exists (select 1 from wa_case_event_messages d "
                    + "              where d.case_id = msg.case_id "
                    + "              and d.event_timestamp > msg.event_timestamp "
                    + "              and not d.from_dlq "
                    + "              and d.state = 'READY') "
                    + "      or exists (select 1 from wa_case_event_messages d "
                    + "                 where d.event_timestamp > msg.event_timestamp + interval '30 minutes' "
                    + "                 and not d.from_dlq "
                    + "                 and d.state in ('READY', 'PROCESSED'))))) "
                    + "and (event_timestamp > hold_until or hold_until is null)"
                    + "for update skip locked "
                    + "limit 1 ";

    String UPDATE_CASE_MESSAGE_STATE =
            "UPDATE public.wa_case_event_messages"
            + " SET state = cast(:#{#messageState.toString()} as message_state_enum)"
            + " WHERE message_id = :messageId";

    String INSERT_CASE_MESSAGE =
            "INSERT into public.wa_case_event_messages as msg (message_id,"
                    + "case_id,"
                    + "event_timestamp,"
                    + "from_dlq,"
                    + "state,"
                    + "message_properties,"
                    + "message_content,"
                    + "received,"
                    + "delivery_count,"
                    + "hold_until,"
                    + "retry_count) "
            + "values (:#{#caseEventMessage.getMessageId()},"
                    + ":#{#caseEventMessage.getCaseId()},"
                    + ":#{#caseEventMessage.getEventTimestamp()},"
                    + ":#{#caseEventMessage.getFromDlq()},"
                    + "cast(:#{#caseEventMessage.getState().toString()} as message_state_enum),"
                    + "to_jsonb(:#{#caseEventMessage.getMessageProperties()}),"
                    + ":#{#caseEventMessage.getMessageContent()},"
                    + ":#{#caseEventMessage.getReceived()},"
                    + ":#{#caseEventMessage.getDeliveryCount()},"
                    + ":#{#caseEventMessage.getHoldUntil()},"
                    + ":#{#caseEventMessage.getRetryCount()}) "
                    + "on conflict (message_id) "
                    + "do update set delivery_count = msg.delivery_count + 1";

    @Query("FROM CaseEventMessageEntity cem WHERE cem.messageId=:messageId")
    List<CaseEventMessageEntity> findByMessageId(String messageId);

    @Query(value = LOCK_AND_GET_NEXT_MESSAGE_SQL, nativeQuery = true)
    CaseEventMessageEntity getNextAvailableMessageReadyToProcess();

    @Modifying
    @Query(value = UPDATE_CASE_MESSAGE_STATE, nativeQuery = true)
    int updateMessageState(@Param("messageState") MessageState messageState, @Param("messageId") String messageId);

    @Modifying
    @Query(value = "UPDATE public.wa_case_event_messages SET retry_count = :retryCount, "
                    + "hold_until = :holdUntil WHERE message_id = :messageId", nativeQuery = true)
    int updateMessageWithRetryDetails(@Param("retryCount") int retryCount,
                                      @Param("holdUntil") LocalDateTime holdUntil,
                                      @Param("messageId") String messageId);

    @Modifying
    @Query(value = INSERT_CASE_MESSAGE, nativeQuery = true)
    void insertCaseEventMessage(@Param("caseEventMessage") CaseEventMessageEntity caseEventMessage);

}
