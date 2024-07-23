TRUNCATE TABLE public.wa_case_event_messages;

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
   ('MessageId_bc8299fc-5d31-45c7-b847-c2622014a85b',
      7,
      '9140-9312-3701-4412',
      '2024-04-02 14:00:00.887100',
      false,
      'NEW',
      'null',
      '{"EventInstanceId":"77838838-2369-443d-a076-9c80bcda17d6","EventTimeStamp":[2022,1,14,12,45,54,887100000],"CaseId":"9140-9312-3701-4413","JurisdictionId":"ia","CaseTypeId":"asylum","EventId":"dummyEvent","PreviousStateId":"","NewStateId":"","UserId":"some user Id","AdditionalData":{"Data":{"lastModifiedDirection":{"dateDue":"","uniqueId":"","directionType":""},"appealType":"protection"},"Definition":null}}',
      '2024-04-02 14:00:00.350754',
      0,
      null,
   0);
