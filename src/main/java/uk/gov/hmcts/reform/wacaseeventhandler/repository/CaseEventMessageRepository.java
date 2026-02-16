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

    String LOCK_AND_GET_NEXT_MESSAGE_SQL = """
        -- Guard clause: if any message has a null case_id, do not select work.
        WITH null_case_id_exists AS (
            SELECT 1 AS exists_flag
            FROM wa_case_event_messages
            WHERE case_id IS NULL
            LIMIT 1
        )
        SELECT *
        FROM public.wa_case_event_messages msg
        -- Candidate messages must already be ready.
        WHERE msg.state = 'READY'
        -- Block processing entirely while null case_id rows exist.
        AND NOT EXISTS (SELECT exists_flag FROM null_case_id_exists)
        -- Skip cases that still have unresolved null event timestamps.
        AND NOT EXISTS (
            SELECT 1 FROM wa_case_event_messages e
            WHERE e.case_id = msg.case_id
            AND e.event_timestamp IS NULL
        )
        -- Enforce in-order processing per case: no older unprocessed message may exist.
        AND NOT EXISTS (
            SELECT 1 FROM wa_case_event_messages older
            WHERE older.case_id = msg.case_id
            AND older.state != 'PROCESSED'
            AND older.event_timestamp < msg.event_timestamp
        )
        -- DLQ messages are allowed only if superseded by newer non-DLQ work
        -- or if enough time has passed with newer non-DLQ activity in the system.
        AND (
            NOT msg.from_dlq
            OR (
                msg.from_dlq AND (
                    EXISTS (
                        SELECT 1 FROM wa_case_event_messages d
                        WHERE d.case_id = msg.case_id
                        AND d.event_timestamp > msg.event_timestamp
                        AND NOT d.from_dlq
                        AND d.state = 'READY'
                    )
                    OR EXISTS (
                        SELECT 1 FROM wa_case_event_messages d
                        WHERE d.event_timestamp > msg.event_timestamp + INTERVAL '30 minutes'
                        AND NOT d.from_dlq
                        AND d.state IN ('READY', 'PROCESSED')
                    )
                )
            )
        )
        -- Respect retry/backoff hold windows.
        AND (current_timestamp > msg.hold_until OR msg.hold_until IS NULL)
        -- Lock one eligible row without blocking concurrent workers.
        FOR UPDATE SKIP LOCKED
        LIMIT 1
        """;

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


    String FIND_PROBLEM_MESSAGES = "SELECT message_id,\n"
        + "sequence,\n"
        + "case_id,\n"
        + "event_timestamp,\n"
        + "from_dlq,\n"
        + "state,\n"
        + "message_properties,\n"
        + "message_content,\n"
        + "received,\n"
        + "delivery_count,\n"
        + "hold_until,\n"
        + "retry_count \n"
        + "from wa_case_event_messages msg \n"
        + "where msg.state IN ('UNPROCESSABLE', 'READY') \n"
        + "and case when msg.state='READY' "
        + "then EXTRACT(EPOCH FROM (((current_timestamp - interval '" + " ?1 minutes')"
        + " - msg.event_timestamp )))/60 "
        + "> ?1 "
        + "else 1=1 end \n"
        + "order by state;";

    String CLEAN_UP_MESSAGES = "DELETE FROM public.wa_case_event_messages cem \n"
        + "WHERE cem.message_id IN (\n"
        + "SELECT msg.message_id \n"
        + "FROM public.wa_case_event_messages msg \n"
        + "WHERE msg.event_timestamp < :timestamp \n"
        + "AND CAST (msg.state AS TEXT) IN(:state) \n"
        + "ORDER BY msg.event_timestamp \n"
        + "LIMIT :limit \n"
        + ");";

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
