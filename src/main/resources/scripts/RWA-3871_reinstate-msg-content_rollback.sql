--Pre checks

-- Result should be 3
select count(*) from public.wa_case_event_messages wcem
where message_id in ('ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794')
  and message_content is null
  and state = 'PROCESSED';

-- Execution


-- case: 1715160767806678
update public.wa_case_event_messages
set message_content = '{"CaseId": "1715160767806678", "UserId": "881c27a4-f5fd-45bb-be40-1e68f935e85f", "EventId": "internal-update-case-summary", "CaseTypeId": "CARE_SUPERVISION_EPO", "NewStateId": "PREPARE_FOR_HEARING", "AdditionalData": {"Data": {}, "Definition": {}}, "EventTimeStamp": "2024-09-26T08:20:13.435806", "JurisdictionId": "PUBLICLAW", "EventInstanceId": 170948742, "PreviousStateId": "PREPARE_FOR_HEARING"}'
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797';

-- case: 1726140730411100
update public.wa_case_event_messages
set message_content = '{"CaseId": "1726140730411100", "UserId": "881c27a4-f5fd-45bb-be40-1e68f935e85f", "EventId": "internal-update-case-summary", "CaseTypeId": "CARE_SUPERVISION_EPO", "NewStateId": "PREPARE_FOR_HEARING", "AdditionalData": {"Data": {}, "Definition": {}}, "EventTimeStamp": "2024-09-26T08:19:46.886105", "JurisdictionId": "PUBLICLAW", "EventInstanceId": 170948720, "PreviousStateId": "PREPARE_FOR_HEARING"}'
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795';

-- case: 1726140730411100
update public.wa_case_event_messages
set message_content = '{"CaseId": "1726140730411100", "UserId": "eac9f0f1-94c8-4f47-b0ef-ca4614cfd7b4", "EventId": "listGatekeepingHearing", "CaseTypeId": "CARE_SUPERVISION_EPO", "NewStateId": "PREPARE_FOR_HEARING", "AdditionalData": {"Data": {"court": {"code": "262", "name": "Family Court sitting at Manchester", "email": null, "region": "North West", "epimmsId": "701411", "regionId": "4", "dateTransferred": null}}, "Definition": {"court": {"type": "Complex", "subtype": "Court", "typeDef": {"code": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "code"}, "name": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "name"}, "email": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "email"}, "region": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "region"}, "epimmsId": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "epimmsId"}, "regionId": {"type": "SimpleText", "subtype": "Text", "typeDef": null, "originalId": "regionId"}, "dateTransferred": {"type": "SimpleDateTime", "subtype": "DateTime", "typeDef": null, "originalId": "dateTransferred"}}, "originalId": "court"}}}, "EventTimeStamp": "2024-09-26T08:19:26.226705", "JurisdictionId": "PUBLICLAW", "EventInstanceId": 170948700, "PreviousStateId": "GATEKEEPING_LISTING"}'
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794';

-- Post checks

-- Result should be 0
select count(*) from public.wa_case_event_messages wcem
where message_id in ('ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794')
  and message_content is null
  and state = 'PROCESSED';

-- Result should be 3
select count(*) from public.wa_case_event_messages wcem
where message_id in ('ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794')
  and message_content is not null
  and state = 'PROCESSED';
