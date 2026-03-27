truncate table public.wa_case_event_messages;

INSERT INTO public.wa_case_event_messages (
  message_id,
  "sequence",
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
)
VALUES
  (
    'ready-older-than-limit',
    20001,
    'case-1',
    current_timestamp - interval '61 minutes',
    false,
    'READY',
    '{}'::jsonb,
    '{"CaseTypeId":"WaCaseType"}',
    current_timestamp - interval '61 minutes',
    1,
    current_timestamp - interval '61 minutes',
    0
  ),
  (
    'ready-at-limit',
    20002,
    'case-2',
    current_timestamp - interval '60 minutes',
    false,
    'READY',
    '{}'::jsonb,
    '{"CaseTypeId":"WaCaseType"}',
    current_timestamp - interval '60 minutes',
    1,
    current_timestamp - interval '60 minutes',
    0
  ),
  (
    'ready-newer-than-limit',
    20003,
    'case-3',
    current_timestamp - interval '59 minutes',
    false,
    'READY',
    '{}'::jsonb,
    '{"CaseTypeId":"WaCaseType"}',
    current_timestamp - interval '59 minutes',
    1,
    current_timestamp - interval '59 minutes',
    0
  ),
  (
    'unprocessable-message',
    20004,
    'case-4',
    current_timestamp - interval '5 minutes',
    false,
    'UNPROCESSABLE',
    '{}'::jsonb,
    '{"CaseTypeId":"WaCaseType"}',
    current_timestamp - interval '5 minutes',
    1,
    current_timestamp - interval '5 minutes',
    0
  );
