CREATE INDEX IF NOT EXISTS idx_wacem_case_ts_not_processed
  ON public.wa_case_event_messages (case_id, event_timestamp)
  WHERE state <> 'PROCESSED';
