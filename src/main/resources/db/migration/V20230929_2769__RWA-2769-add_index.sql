CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_wa_case_event_messages_state_dlq_event_timestamp on public.wa_case_event_messages(state, from_dlq, event_timestamp);
