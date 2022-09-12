INSERT INTO public.wa_case_event_messages (message_id,
                                           sequence,
                                           case_id,
                                           event_timestamp,
                                           from_dlq,
                                           state,
                                           message_properties,
                                           message_content,
                                           received,
                                           delivery_count,
                                           hold_until,
                                           retry_count)
VALUES
  ('MessageId_30915063-ec4b-4272-933d-91087b486195',
   4,
   '6761-0650-5813-1570',
   '2022-01-04 12:41:16.162368',
   false,
   'NEW',
   'null',
   '{"EventInstanceId":"3bc668cd-3da3-4968-9936-7a679f9f084a","EventTimeStamp":[2022,1,4,12,41,16,162368000],"CaseId":"6761-0650-5813-1570","JurisdictionId":"jurisdictionid","CaseTypeId":"casetypeid","EventId":"dummyEvent","PreviousStateId":"","NewStateId":"","UserId":"some user Id","AdditionalData":{"Data":{"lastModifiedDirection":{"uniqueId":"","directionType":"","dateDue":""},"appealType":"protection"},"Definition":null}}',
   '2022-08-26 11:30:00.458704',
   0,
   null,
   0),

  ('MessageId_6cecf982-6b9e-4cc3-a8f5-d04b1385c258',
   5,
   '8375-3716-6885-2639',
   '2022-01-04 12:42:46.006026',
   true,
   'NEW',
   'null',
   '{"EventInstanceId":"5408b0d9-b780-4bf5-b4e7-37f491d7c383","EventTimeStamp":[2022,1,4,12,42,46,6026000],"CaseId":"8375-3716-6885-2639","JurisdictionId":"jurisdictionid","CaseTypeId":"casetypeid","EventId":"dummyEvent","PreviousStateId":"","NewStateId":"","UserId":"some user Id","AdditionalData":{"Data":{"appealType":"protection","lastModifiedDirection":{"uniqueId":"","directionType":"","dateDue":""}},"Definition":null}}',
   '2022-08-26 11:31:00.563920',
   0,
   null,
   0),

  ('MessageId_bc8299fc-5d31-45c7-b847-c2622014a85a',
   6,
   '9140-9312-3701-4412',
   '2022-01-14 12:45:54.887100',
   false,
   'READY',
   'null',
   '{"EventInstanceId":"77838838-2369-443d-a076-9c80bcda17d5","EventTimeStamp":[2022,1,14,12,45,54,887100000],"CaseId":"9140-9312-3701-4412","JurisdictionId":"jurisdictionid","CaseTypeId":"casetypeid","EventId":"dummyEvent","PreviousStateId":"","NewStateId":"","UserId":"some user Id","AdditionalData":{"Data":{"lastModifiedDirection":{"dateDue":"","uniqueId":"","directionType":""},"appealType":"protection"},"Definition":null}}',
   '2022-08-26 14:00:00.350754',
   0,
   '2022-01-04 12:45:54.887100',
   0);
