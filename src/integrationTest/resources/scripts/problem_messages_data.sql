/*
 Tests share the same container and depending on the order of execution, the unhappy path  tests can fail,
 that is the reason for the deletion of happy path records before running this test.
 */
truncate table public.wa_case_event_messages;
INSERT INTO public.wa_case_event_messages (message_id,"sequence",case_id, event_timestamp,from_dlq, state,message_properties,message_content,received,delivery_count,hold_until,retry_count)
VALUES('8d6cc5cf-c973-11eb-bdba-0242ac111000',12345,'1623278362431000','2022-04-09T20:15:45.345875+01:00',false,'READY','{}','','2022-04-09T20:15:45.345875+01:00',1,'2022-04-09T20:15:45.345875+01:00',0),
      ('8d6cc5cf-c973-11eb-bdba-0242ac111001',12346,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'UNPROCESSABLE','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('8d6cc5cf-c973-11eb-bdba-0242ac111002',12347,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'UNPROCESSABLE','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('8d6cc5cf-c973-11eb-bdba-0242ac111003',12348,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'NEW','{}','','2022-05-09T20:15:45.345875+01:00',1,'2022-05-09T20:15:45.345875+01:00',0),
      ('8d6cc5cf-c973-11eb-bdba-0242ac111004',12349,'1623278362431000','2022-05-09T20:15:45.345875+01:00',false,'PROCESSED','{}','','2022-04-09T20:15:45.345875+01:00',1,'2022-04-09T20:15:45.345875+01:00',0);
