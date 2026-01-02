create table if not exists captive.user_identity
(
    id            uuid primary key,
    user_id       uuid        not null,
    issuer        text        not null,
    subject       text        not null,
    display_name  text        not null,
    email         text        not null,
    picture_url   text        null,
    roles         text[]      not null default '{}',
    created_at    timestamptz not null,
    unique (issuer, subject)
);

create index if not exists idx_captive_user_identity_user_id on captive.user_identity (user_id);
create index if not exists idx_captive_user_identity_issuer_subject on captive.user_identity (issuer, subject);
