create index idx_wa_case_event_messages_ready_case_id_event_timestamp on public.wa_case_event_messages(case_id, event_timestamp)
where state = 'READY';

create index idx_wacem_case_ts_not_processed on public.wa_case_event_messages(case_id, event_timestamp)
where state <> 'PROCESSED';
