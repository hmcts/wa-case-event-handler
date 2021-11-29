CREATE TYPE message_state_enum as ENUM ('NEW', 'READY', 'PROCESSED', 'UNPROCESSABLE');

CREATE TABLE public.wa_case_event_messages(
      message_id text NOT NULL PRIMARY KEY,
      sequence serial,
      case_id text NOT NULL,
      event_timestamp timestamp,
      from_dlq boolean NOT NULL DEFAULT false,
      state message_state_enum NOT NULL,
      message_properties jsonb,
      message_content text,
      received timestamp NOT NULL,
      delivery_count integer NOT NULL DEFAULT 1,
      hold_until timestamp,
      retry_count integer NOT NULL DEFAULT 0
);