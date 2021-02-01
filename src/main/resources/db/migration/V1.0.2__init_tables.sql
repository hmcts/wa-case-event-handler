SET
search_path TO wa_case_event_handler;

DROP TABLE IF EXISTS idempotent_keys;

CREATE TABLE idempotent_keys
(
    idempotency_key varchar(200) NOT NULL,
    tenant_id       varchar(20)  NOT NULL,
    process_id      varchar(200) NOT NULL,
    created_at TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    last_updated_at TIMESTAMP NOT NULL default CURRENT_TIMESTAMP,
    PRIMARY KEY (idempotency_key, tenant_id));

COMMIT;
