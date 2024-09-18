/*
 Tests share the same container and depending on the order of execution, the unhappy path  tests can fail,
 that is the reason for the deletion of happy path records before running this test.
 */
truncate table public.wa_case_event_messages;
INSERT INTO public.wa_case_event_messages (message_id,"sequence",case_id, event_timestamp,from_dlq, state,message_properties,message_content,received,delivery_count,hold_until,retry_count)
VALUES('ID:c05439ca-ddb2-47d0-a0a6-ba9db76a3064:58:1:1-10',12345,'1623278362431000','2022-04-09T20:15:45.345875+01:00',false,'READY','{}','','2022-04-09T20:15:45.345875+01:00',1,'2022-04-09T20:15:45.345875+01:00',0),
      ('ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-2704',12346,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'UNPROCESSABLE','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('ID:ce8467a0-cea9-4a65-99dd-3ae9a94a4453:16:1:1-811',12347,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'UNPROCESSABLE','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('ID:04b1809e-c7ca-47d2-90ff-fc95c2e110ab:54:1:1-781',12348,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'NEW','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:1-1675',12349,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'PROCESSED','{}','','2022-04-09T20:15:45.345875+01:00',1,'2022-04-09T20:15:45.345875+01:00',0),
      ('ID:d257fa4f-73ad-4a82-a30e-9acc377f593d:1:1:2-1675',12350,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'READY','{}','{"EventInstanceId":"d7ebb30c-8b48-4edf-9e16-f4735b13b214"}','2022-04-09T20:15:45.345875+01:00',1,'2022-04-09T20:15:45.345875+01:00',0);
