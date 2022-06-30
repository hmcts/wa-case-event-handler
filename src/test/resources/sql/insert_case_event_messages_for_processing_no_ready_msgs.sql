INSERT INTO public.wa_case_event_messages (message_id,
                                           case_id,
                                           event_timestamp,
                                           from_dlq,
                                           state,
                                           message_content,
                                           received)
VALUES
  ('MessageId_30915063-ec4b-4272-933d-91087b486195',
   '6761-0650-5813-1570',
   '2022-01-04 12:41:16.162368',
   false,
   'NEW',
   '{"UserId":"some user Id"}',
   '2022-01-05 12:41:19.458704'),

  ('MessageId_6cecf982-6b9e-4cc3-a8f5-d04b1385c258',
   '8375-3716-6885-2639',
   '2022-01-04 12:42:46.006026',
   true,
   'NEW',
   '{"UserId":"some user Id"}',
   '2022-01-05 12:42:48.563920'),

  ('MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a',
   '9140-9312-3701-4412',
   '2022-01-14 12:45:54.887100',
   false,
   'NEW',
   '{"UserId":"some user Id"}',
   '2022-01-05 12:45:58.350754');
