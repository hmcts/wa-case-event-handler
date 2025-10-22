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
        // earliest event message unprocessed message for the case
        + "and (msg.case_id, msg.event_timestamp) in ( "
        + "  select case_id, min(event_timestamp) "
        + "  from wa_case_event_messages "
        + "  where state != 'PROCESSED' "
        + "  group by case_id) "
        // there is no event message for the same case with timestamp null
        + "and not exists (select 1 from wa_case_event_messages e "
        + "                where e.case_id = msg.case_id "
        + "                and e.event_timestamp is null) "
        // there is no any event message with case id null
        + "and not exists (select 1 from wa_case_event_messages c "
        + "                where c.case_id is null) "
        + "and ( "
        + "  not msg.from_dlq "
        + "  or ( "
        // if message from dlq
        + "    msg.from_dlq and ( "
        // There is at least one non dlq message in ready state and higher timestamp for the same case
        + "      exists (select 1 from wa_case_event_messages d "
        + "              where d.case_id = msg.case_id "
        + "              and d.event_timestamp > msg.event_timestamp "
        + "              and not d.from_dlq "
        + "              and d.state = 'READY') "
        // There is at least one non dlq message in ready or processed state and timestamp is 30 min higher
        + "      or exists (select 1 from wa_case_event_messages d "
        + "                 where d.event_timestamp > msg.event_timestamp + interval '30 minutes' "
        + "                 and not d.from_dlq "
        + "                 and d.state in ('READY', 'PROCESSED'))))) "
        + "and (current_timestamp > hold_until or hold_until is null)"
        + "for update skip locked "
        + "limit 1 ";

    String UPDATE_CASE_MESSAGE_STATE =
        "UPDATE public.wa_case_event_messages"
        + " SET state = cast(:#{#messageState.toString()} as message_state_enum)"
        + " WHERE message_id in (:messageIds)";

    String UPDATE_CASE_MESSAGE_RETRY_DETAILS =
        "UPDATE public.wa_case_event_messages SET retry_count = :retryCount, "
        + "hold_until = :holdUntil WHERE message_id = :messageId";

    String SELECT_NEW_MESSAGES =
        "SELECT * from public.wa_case_event_messages msg where msg.state = 'NEW' "
        + "order by sequence DESC for update skip locked";


    String FIND_PROBLEM_MESSAGES = """
        SELECT message_id,
        sequence,
        case_id,
        event_timestamp,
        from_dlq,
        state,
        message_properties,
        message_content,
        received,
        delivery_count,
        hold_until,
        retry_count
        from wa_case_event_messages msg
        where msg.state IN ('UNPROCESSABLE', 'READY')
        and case when msg.state='READY' then
        EXTRACT(EPOCH FROM (((current_timestamp - interval '?1 minutes') - msg.event_timestamp)))/60 > ?1
        else 1=1 end
        order by state;""";

    String CLEAN_UP_MESSAGES = """
        DELETE FROM public.wa_case_event_messages cem
        WHERE cem.message_id IN (
        SELECT msg.message_id
        FROM public.wa_case_event_messages msg
        WHERE msg.event_timestamp < :timestamp
        AND CAST (msg.state AS TEXT) IN(:state)
        ORDER BY msg.event_timestamp
        LIMIT :limit
        );""";

    String GET_NUMBER_MESSAGES_RECEIVED_IN_LAST_HOUR = "SELECT count(*) from "
                                                       + "wa_case_event_messages where received > :timestamp";

    String GET_MESSAGES_IN_NEW_STATE = "SELECT count(*) from wa_case_event_messages msg where msg.state = 'NEW'";

    @Query("FROM CaseEventMessageEntity cem WHERE cem.messageId IN (:messageIds)")
    List<CaseEventMessageEntity> findByMessageId(List<String> messageIds);

    @Query(value = LOCK_AND_GET_NEXT_MESSAGE_SQL, nativeQuery = true)
    CaseEventMessageEntity getNextAvailableMessageReadyToProcess();

    @Modifying
    @Query(value = UPDATE_CASE_MESSAGE_STATE, nativeQuery = true)
    int updateMessageState(@Param("messageState") MessageState messageState,
                           @Param("messageIds") List<String> messageIds);

    @Modifying
    @Query(value = UPDATE_CASE_MESSAGE_RETRY_DETAILS, nativeQuery = true)
    int updateMessageWithRetryDetails(@Param("retryCount") int retryCount,
                                      @Param("holdUntil") LocalDateTime holdUntil,
                                      @Param("messageId") String messageId);

    @Query(value = SELECT_NEW_MESSAGES, nativeQuery = true)
    List<CaseEventMessageEntity> getAllMessagesInNewState();

    @Query(value = FIND_PROBLEM_MESSAGES, nativeQuery = true)
    List<CaseEventMessageEntity> findProblemMessages(int messageTimeLimit);

    @Query(value = GET_NUMBER_MESSAGES_RECEIVED_IN_LAST_HOUR, nativeQuery = true)
    int getNumberOfMessagesReceivedInLastHour(@Param("timestamp") LocalDateTime timestamp);

    @Query(value = GET_MESSAGES_IN_NEW_STATE, nativeQuery = true)
    int getNumberOfMessagesInNewState();

    @Modifying
    @Query(value = CLEAN_UP_MESSAGES, nativeQuery = true)
    void removeOldMessages(
        @Param("limit") int limit,
        @Param("state") List<String> state,
        @Param("timestamp") LocalDateTime timestamp
    );

}
