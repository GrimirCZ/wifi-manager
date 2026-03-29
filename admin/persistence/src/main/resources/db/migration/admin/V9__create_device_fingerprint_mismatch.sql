create table if not exists admin.device_fingerprint_mismatch
(
    id                   uuid primary key,
    subject_type         text        not null,
    user_id              uuid        null,
    ticket_id            uuid        null,
    device_mac           text        not null,
    score                integer     not null,
    breached             boolean     not null,
    action_taken         text        not null,
    reasons              text[]      not null default '{}',
    previous_fingerprint jsonb       null,
    current_fingerprint  jsonb       null,
    previous_sources     jsonb       null,
    current_sources      jsonb       null,
    detected_at          timestamptz not null
);

create index if not exists idx_admin_device_fingerprint_mismatch_device_mac
    on admin.device_fingerprint_mismatch (device_mac);

create index if not exists idx_admin_device_fingerprint_mismatch_detected_at
    on admin.device_fingerprint_mismatch (detected_at desc);
