create index idx_wa_case_event_messages_case_id_event_timestamp on public.wa_case_event_messages(case_id, event_timestamp);

create index idx_wa_case_event_messages_case_id_state_event_timestamp on public.wa_case_event_messages(case_id, state, event_timestamp);

create index idx_wa_case_event_messages_state_from_dlq_case_id_event_timestamp on public.wa_case_event_messages(state, from_dlq, case_id, event_timestamp);