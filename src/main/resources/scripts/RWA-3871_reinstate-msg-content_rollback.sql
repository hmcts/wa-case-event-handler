--Pre checks

-- Result should be 0
select count(*) from public.wa_case_event_messages wcem
where message_id in ('ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794')
  and message_content is null
  and state = 'PROCESSED';

-- Execution


-- case: 1715160767806678
update public.wa_case_event_messages
set message_content = null
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797';

-- case: 1726140730411100
update public.wa_case_event_messages
set message_content = null
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795';

-- case: 1726140730411100
update public.wa_case_event_messages
set message_content = null
where message_id = 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794';

-- Post checks

-- Result should be 3
select count(*) from public.wa_case_event_messages wcem
where message_id in ('ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-797', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-795', 'ID:3e202151-b9d2-4a1e-91f7-5b1bc17bd80e:313:1:1-794')
  and message_content is null
  and state = 'PROCESSED';

